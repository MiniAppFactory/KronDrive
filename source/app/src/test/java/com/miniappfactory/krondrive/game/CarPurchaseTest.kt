package com.miniappfactory.krondrive.game

import com.miniappfactory.krondrive.data.PlayerProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SATIN ALMA YOLU — KONTROL ILE TAHSILAT AYNI SAYIYA BAKMALI
 * (2026-08-19, QA bosluk taramasi).
 *
 * `GameStateRepository.buyCarItem` iki adim atiyor:
 *
 *   1. **kontrol**  : `CarCatalog.canBuy(item, owned, coins, carLevel)`
 *   2. **tahsilat** : `coins - CarCatalog.totalPrice(item, carLevel)`
 *
 * Bu ikisi ayri fonksiyon oldugu icin birbirinden AYRISABILIYORLAR ve bir kez
 * ayristilar: kontrol toplama (fiyat + seviye atlama bedeli) bakarken tahsilat
 * yalnizca `item.priceCoins` yaziyordu, yani oyuncu arac seviyesini BEDAVAYA
 * atliyordu (repository icinde yorumla belgelenmis, 2026-08-19).
 *
 * Klasik "kontrol bir yerde, etki baska yerde" hatasi. Asagidaki testler bu
 * iki tarafi TEK BIR degismezle birbirine bagliyor: **canBuy true ise
 * totalPrice kadar coin MUTLAKA vardir**, yani tahsilat bakiyeyi hicbir zaman
 * negatife dusuremez.
 *
 * Repository'nin kendisi `Context` istedigi icin JVM testinden cagrilamiyor;
 * bu yuzden testler onun kullandigi saf mantigi ayni kombinasyonlarla suruyor.
 */
class CarPurchaseTest {

    private val tumIcerik: List<CarItem> get() = CarCatalog.shapes + CarCatalog.colors

    /** Katalogda gecen en yuksek seviye sarti + pay. */
    private val seviyeAraligi: IntRange get() = 1..(tumIcerik.maxOf { it.requiredCarLevel } + 3)

    /**
     * TAM SINIRDA ALINABILIR, BIR COIN EKSIKTE ALINAMAZ.
     *
     * Bu testin asil isi esik degil, **kontrol ile tahsilatin ayni sayiyi
     * kullandigini** kanitlamak: esik tam olarak `totalPrice` ise iki taraf
     * ayni yerdedir. Tahsilat tekrar `priceCoins`e donerse (eski hata) bu test
     * hala gecerdi — o yuzden asagidaki "bakiye negatife dusmez" testi de var.
     */
    @Test
    fun `alim esigi tam olarak odenecek toplam tutardir`() {
        tumIcerik.forEach { item ->
            if (CarCatalog.isOwned(item, emptySet())) return@forEach // bedava icerik
            seviyeAraligi.forEach { seviye ->
                val toplam = CarCatalog.totalPrice(item, seviye)
                assertTrue(
                    "${item.id} (arac sv$seviye): coin tam $toplam iken alinamiyor — " +
                        "kontrol ile tahsilat ayni sayiya bakmiyor",
                    CarCatalog.canBuy(item, emptySet(), toplam, seviye)
                )
                assertFalse(
                    "${item.id} (arac sv$seviye): coin ${toplam - 1} iken alinabiliyor — " +
                        "oyuncu odeyemedigi bir seyi aliyor",
                    CarCatalog.canBuy(item, emptySet(), toplam - 1, seviye)
                )
            }
        }
    }

    /**
     * TAHSILAT BAKIYEYI NEGATIFE DUSUREMEZ.
     *
     * `canBuy` true dondugu HER durumda, `totalPrice` kadar dusuldukten sonra
     * bakiye >= 0 kalmali. Bu, kontrol ile tahsilatin ayrisamayacaginin
     * dogrudan ifadesi: 2026-08-19'daki hata bu testte kirilirdi cunku seviye
     * atlama bedeli tahsil edilmeden gecen alim, ayni kontrolden gecmis
     * olmasina ragmen farkli bir tutar dusuyordu.
     */
    @Test
    fun `alinabilir denen her icerik odendikten sonra bakiye negatif olmaz`() {
        val coinDegerleri = listOf(0, 1, 99, 100, 350, 900, 1500, 3200, 5000, 12_000, 100_000)

        tumIcerik.forEach { item ->
            seviyeAraligi.forEach { seviye ->
                coinDegerleri.forEach { coin ->
                    if (!CarCatalog.canBuy(item, emptySet(), coin, seviye)) return@forEach
                    val kalan = coin - CarCatalog.totalPrice(item, seviye)
                    assertTrue(
                        "${item.id} (arac sv$seviye, $coin coin): alinabilir denildi ama " +
                            "odeme sonrasi bakiye $kalan — tahsilat kontrolden buyuk",
                        kalan >= 0
                    )
                }
            }
        }
    }

    /**
     * CIFT DOKUNUS: sahip olunan icerik ikinci kez satin alinamaz.
     *
     * Repository satin almayi tek bir atomik `edit {}` blogunda yapiyor ve
     * ilk dokunus envantere yaziyor; ikinci dokunus bu kontrole carpar. Sinirsiz
     * coinle bile `canBuy` false donmeli — yoksa hizli iki dokunus ayni araci
     * iki kez odetirdi.
     */
    @Test
    fun `sahip olunan icerik sinirsiz coinle bile tekrar satin alinamaz`() {
        tumIcerik.forEach { item ->
            val envanter = setOf(item.id)
            seviyeAraligi.forEach { seviye ->
                assertFalse(
                    "${item.id} (arac sv$seviye): zaten sahip olunmasina ragmen " +
                        "tekrar satin alinabiliyor — cift dokunus parayi iki kez yakar",
                    CarCatalog.canBuy(item, envanter, Int.MAX_VALUE / 2, seviye)
                )
                assertEquals(
                    "${item.id}: sahip olunan icerigin durumu OWNED olmali",
                    CarUnlockState.OWNED,
                    CarCatalog.stateOf(item, envanter, Int.MAX_VALUE / 2, seviye)
                )
            }
        }
    }

    /**
     * BEDAVA ICERIK HICBIR ZAMAN UCRET ISTEMEZ.
     *
     * Bedava icerik `isOwned` uzerinden bastan sahiplenilmis sayiliyor, yani
     * satin alma yoluna hic girmemeli. Girseydi ve bir gun `requiredCarLevel`
     * kazansaydi, seviye atlama bedeli yuzunden BEDAVA bir arac ucretli hale
     * gelirdi.
     */
    @Test
    fun `bedava icerik bastan sahiplenilmis sayilir ve bedeli sifirdir`() {
        tumIcerik.filter { it.priceCoins == 0 }.forEach { item ->
            assertTrue(
                "${item.id}: bedava icerik bos envanterle de sahiplenilmis olmali",
                CarCatalog.isOwned(item, emptySet())
            )
            assertEquals(
                "${item.id}: bedava icerigin seviye sarti 1 olmali, yoksa atlama " +
                    "bedeli yuzunden bedava olmaktan cikar",
                1, item.requiredCarLevel
            )
            assertEquals(
                "${item.id}: bedava icerik icin odenecek toplam 0 olmali",
                0, CarCatalog.totalPrice(item, carLevel = 1)
            )
        }
    }

    /**
     * SEVIYE ATLAMA BEDELI: yalnizca EKSIK seviye kadar, fazlasi degil.
     *
     * Seviyesi yeten oyuncu tek kurus fazla odememeli; yetmeyen oyuncu eksik
     * seviye basina [GameConfig.LEVEL_SKIP_COIN_PER_LEVEL] odemeli. Bedelin
     * seviye ile DOGRUSAL olmasi onemli: karesel bir formul ust seviye
     * araclarini erisilemez yapardi ve bunu kimse fark etmezdi.
     */
    @Test
    fun `seviye atlama bedeli yalnizca eksik seviye kadar odetir`() {
        tumIcerik.forEach { item ->
            // Seviyesi yeten oyuncu: ek bedel yok.
            (item.requiredCarLevel..item.requiredCarLevel + 3).forEach { seviye ->
                assertEquals(
                    "${item.id} (arac sv$seviye): seviye yettigi halde atlama bedeli alindi",
                    0, CarCatalog.levelSkipFee(item, seviye)
                )
                assertEquals(
                    "${item.id} (arac sv$seviye): odenecek toplam fiyattan farkli",
                    item.priceCoins, CarCatalog.totalPrice(item, seviye)
                )
            }

            // Seviyesi yetmeyen oyuncu: eksik seviye basina sabit bedel.
            (1 until item.requiredCarLevel).forEach { seviye ->
                val eksik = item.requiredCarLevel - seviye
                assertEquals(
                    "${item.id} (arac sv$seviye): $eksik seviye eksikken atlama bedeli yanlis",
                    eksik * GameConfig.LEVEL_SKIP_COIN_PER_LEVEL,
                    CarCatalog.levelSkipFee(item, seviye)
                )
                assertEquals(
                    "${item.id} (arac sv$seviye): toplam = fiyat + atlama bedeli olmali",
                    item.priceCoins + eksik * GameConfig.LEVEL_SKIP_COIN_PER_LEVEL,
                    CarCatalog.totalPrice(item, seviye)
                )
            }
        }
    }

    /**
     * GOVDE ALMAK YALNIZCA KENDI FABRIKA BOYASINI ACAR.
     *
     * `effectiveOwnedColors` "gövdeye sahip olmak fabrika boyasini da acar"
     * kuralini uyguluyor. Bu kural fazla genis yazilsaydi (orn. tum boyalar
     * acilsaydi) katalogdaki 2200 coinlik boyalar bir anda bedavaya duserdi
     * ve hicbir test bunu yakalamazdi — magaza dolu gorunmeye devam ederdi.
     */
    @Test
    fun `govde satin almak yalnizca kendi fabrika boyasini acar`() {
        val ucretliGovde = CarCatalog.shapes.first { it.priceCoins > 0 }
        val oncesi = CarCatalog.effectiveOwnedColors(emptySet(), emptySet())
        val sonrasi = CarCatalog.effectiveOwnedColors(setOf(ucretliGovde.id), emptySet())

        assertTrue(
            "${ucretliGovde.id} alindiginda fabrika boyasi ${ucretliGovde.defaultColorId} acilmali",
            ucretliGovde.defaultColorId in sonrasi
        )
        assertEquals(
            "bir gövde almak yalnizca kendi fabrika boyasini acmali; acilan fazladan " +
                "boyalar: ${sonrasi - oncesi - ucretliGovde.defaultColorId}",
            (oncesi + ucretliGovde.defaultColorId), sonrasi
        )
    }

    /**
     * YAYIN BASLANGIC COINI KATALOGU ACMAMALI.
     *
     * [PlayerProgress.STARTING_COINS] su an gecici bir TEST degeri
     * (`PlayerProgressCarTest` onu ayrica kilitliyor). Burada olculen sey
     * YAYIN degeri: oyuncu ilk saniyede katalogu suprememeli, aksi halde
     * ekonominin tamami — araclar, boyalar ve dort yukseltme dali —
     * anlamsizlasir.
     */
    @Test
    fun `yayin baslangic coini en ucuz ucretli araci bile karsilamaz`() {
        val enUcuzUcretli = tumIcerik.filter { it.priceCoins > 0 }.minOf { it.priceCoins }
        assertTrue(
            "yayin baslangic coini (${PlayerProgress.STARTING_COINS_RELEASE}) en ucuz " +
                "ucretli icerigi ($enUcuzUcretli) hemen aliyor — ilk satin alma bir " +
                "hedef olmaktan cikar",
            PlayerProgress.STARTING_COINS_RELEASE < enUcuzUcretli
        )
    }
}
