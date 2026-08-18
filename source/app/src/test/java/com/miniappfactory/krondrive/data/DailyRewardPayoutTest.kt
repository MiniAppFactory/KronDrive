package com.miniappfactory.krondrive.data

import com.miniappfactory.krondrive.game.GameConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GUNLUK GOREV ODEMESI — AYNI GUN TEKRAR OYNAMA (2026-08-19, QA taramasi).
 *
 * Gunluk gorev gunde bir kez DEGIL, istenildigi kadar oynanabiliyor; gun
 * icinde kademeler yalnizca BIR KEZ odenmeli. Odeme mantigi iki parcada:
 *
 *  - [DailyChallenge.rewardBetween] — hangi araligin odenecegi (saf)
 *  - `GameStateRepository.grantDailyTiers` — kayitli kademeyi okuyup
 *    guncelleyen atomik islem (Context istedigi icin JVM'den cagrilamiyor)
 *
 * `DailyChallengeGeneratorTest` sablonlarin BICIMINI dogruluyor (artan hedef,
 * artan odul). Burada dogrulanan sey bicim degil **para akisi**: bir gun
 * boyunca yapilan tum kosularin toplam odemesi gunluk tavani asamaz.
 */
class DailyRewardPayoutTest {

    /** Uretilen tum sablonlar (gun kimliginden turetiliyor). */
    private val sablonlar: List<DailyChallenge> =
        (1..12).flatMap { ay -> (1..28).map { gun -> "2026-%02d-%02d".format(ay, gun) } }
            .map { DailyChallengeGenerator.forDay(it) }
            .distinctBy { it.id }

    /**
     * GUN BOYUNCA TOPLAM ODEME TAVANI ASMAZ.
     *
     * Oyuncunun gun icindeki kosulari her seferinde bir onceki en iyi
     * kademeyi asarak ilerlese bile, odenen coinlerin TOPLAMI tam olarak
     * `totalRewardCoins` olmali. Bu, "her kosu bastan oder" ya da "kademe
     * ikinci kez sayilir" turunden bir hatayi yakalar.
     */
    @Test
    fun `gun icinde tekrar oynamak toplam odulu asamaz`() {
        sablonlar.forEach { sablon ->
            val kademeSayisi = sablon.tiers.size
            var odenmis = 0
            var toplam = 0

            // Oyuncu gun icinde sirasiyla 1, 2, ... kademeye ulasiyor.
            (1..kademeSayisi).forEach { ulasilan ->
                toplam += sablon.rewardBetween(odenmis, ulasilan)
                odenmis = ulasilan
            }

            assertEquals(
                "${sablon.id}: gun boyunca odenen toplam gunluk odulden farkli",
                sablon.totalRewardCoins, toplam
            )
        }
    }

    /**
     * DAHA KOTU BITEN IKINCI KOSU PARA URETMEZ VE GERI ALMAZ.
     *
     * Oyuncu once ucuncu kademeye ulasip sonra ayni gun daha kotu bir kosu
     * yaparsa: ikinci kosu ne coin odemeli ne de verdigini geri almali.
     * Negatif bir sonuc repository'de coin dusurmeye donusurdu.
     */
    @Test
    fun `daha dusuk kademeyle biten ikinci kosu ne oder ne geri alir`() {
        sablonlar.forEach { sablon ->
            val son = sablon.tiers.size
            (0..son).forEach { odenmis ->
                (0..odenmis).forEach { ulasilan ->
                    val odeme = sablon.rewardBetween(odenmis, ulasilan)
                    assertEquals(
                        "${sablon.id}: $odenmis kademe odenmisken $ulasilan kademeye " +
                            "ulasan kosu $odeme coin uretti — 0 olmaliydi",
                        0, odeme
                    )
                }
            }
        }
    }

    /**
     * ODEME HER ZAMAN POZITIF VE ARTIMLI.
     *
     * Odenmis kademe sayisi arttikca ayni hedefe ulasmanin odemesi AZALMALI
     * (kalan kademeler azaliyor) ve hicbir kombinasyon negatif uretmemeli.
     */
    @Test
    fun `odeme hicbir kademe kombinasyonunda negatif olmaz`() {
        sablonlar.forEach { sablon ->
            val son = sablon.tiers.size
            (0..son).forEach { odenmis ->
                var oncekiOdeme = Int.MAX_VALUE
                (son downTo odenmis).forEach { ulasilan ->
                    val odeme = sablon.rewardBetween(odenmis, ulasilan)
                    assertTrue(
                        "${sablon.id}: rewardBetween($odenmis, $ulasilan) = $odeme negatif",
                        odeme >= 0
                    )
                    assertTrue(
                        "${sablon.id}: daha az kademeye ulasmak daha cok odedi " +
                            "($ulasilan -> $odeme, bir onceki $oncekiOdeme)",
                        odeme <= oncekiOdeme
                    )
                    oncekiOdeme = odeme
                }
            }
        }
    }

    /**
     * GUNLUK DURUM KENDI ICINDE TUTARLI.
     *
     * Arayuz uc tureti alani birden okuyor: `completed`, `nextTier`,
     * `remainingCoins`. Ucu birbiriyle celisirse ekran "gorev bitti" derken
     * "kalan 280 coin" gosterir. Bu, kademe sayisi degistiginde
     * ([DailyChallenge.TIER_COUNT]) kolayca olusabilecek bir tutarsizlik.
     */
    @Test
    fun `gunluk durum her kademede kendi icinde tutarli`() {
        sablonlar.forEach { sablon ->
            (0..sablon.tiers.size).forEach { odenmis ->
                val durum = DailyChallengeState("2026-08-19", sablon, tiersGranted = odenmis)
                val bitti = odenmis >= sablon.tiers.size

                assertEquals(
                    "${sablon.id} ($odenmis kademe): completed yanlis", bitti, durum.completed
                )
                if (bitti) {
                    assertNull("${sablon.id}: bitmis gorevde sonraki kademe olmamali", durum.nextTier)
                    assertEquals(
                        "${sablon.id}: bitmis gorevde kalan coin 0 olmali",
                        0, durum.remainingCoins
                    )
                } else {
                    assertNotNull(
                        "${sablon.id} ($odenmis kademe): bitmemis gorevde sonraki kademe olmali",
                        durum.nextTier
                    )
                    assertTrue(
                        "${sablon.id} ($odenmis kademe): bitmemis gorevde kalan coin 0",
                        durum.remainingCoins > 0
                    )
                }
                assertEquals(
                    "${sablon.id} ($odenmis kademe): odenen + kalan gunluk toplama esit olmali",
                    sablon.totalRewardCoins,
                    sablon.rewardBetween(0, odenmis) + durum.remainingCoins
                )
            }
        }
    }

    /**
     * SAAT GERI ALINDIGINDA GUNLUK GOREV YENIDEN ODENMEZ.
     *
     * `GameStateRepository` iki parcayi birlestiriyor: gunun tazeligi
     * ([GameStateRepository.isFreshDay]) ve odenmis kademe sayisi. `DailyResetTest`
     * yalnizca birinci parcayi test ediyor; bu test ikisini BIRLIKTE surerek
     * asil sonucu — **kac coin odenir** — dogruluyor.
     *
     * Senaryo: oyuncu bugun tum kademeleri alir, sonra cihaz saatini dune
     * ceker ve gunluk gorevi tekrar oynar. Kayitli gun ILERIDE oldugu icin
     * sayac sifirlanmaz, yani ikinci gun 0 coin odenir.
     */
    @Test
    fun `saat geri alinirsa gunluk gorev ikinci kez odenmez`() {
        val sablon = DailyChallengeGenerator.forDay("2026-08-19")
        val kademeSayisi = sablon.tiers.size

        val kayitliGun = "2026-08-19"
        val geriAlinmisGun = "2026-08-18"
        val gercekYeniGun = "2026-08-20"

        // Repository'nin `grantedTiers` kararinin ayni mantikla yeniden kurulmasi.
        fun odenecek(bugun: String, kayitliKademe: Int, ulasilan: Int): Int {
            val odenmis = if (GameStateRepository.isFreshDay(kayitliGun, bugun)) 0 else kayitliKademe
            return if (ulasilan > odenmis) sablon.rewardBetween(odenmis, ulasilan) else 0
        }

        assertEquals(
            "saat geri alindiginda gunluk gorev yeniden odendi — sinirsiz coin",
            0, odenecek(geriAlinmisGun, kademeSayisi, kademeSayisi)
        )
        assertEquals(
            "ayni gun tekrar oynamak yeniden odendi",
            0, odenecek(kayitliGun, kademeSayisi, kademeSayisi)
        )
        assertEquals(
            "GERCEKTEN yeni gunde gunluk gorev yeniden odenmeliydi",
            sablon.totalRewardCoins, odenecek(gercekYeniGun, kademeSayisi, kademeSayisi)
        )
    }

    /**
     * Gunluk gorevin gunluk tavani, kariyer ilerlemesini anlamsizlastirmamali.
     * Sabitin basindaki gerekce (2026-08-16) bir bolumun ortalama **100 coin**
     * odedigini olcmus ve gunluk toplami 900'den 500'e indirmisti. Bu test o
     * karari sayiya bagliyor: gunluk gorev en fazla birkac bolum degerinde
     * olmali, yoksa "gunde bir kez gir, cik" oynamaktan karli hale gelir.
     */
    @Test
    fun `gunluk gorev odulu birkac bolumluk ilerlemeyi asmaz`() {
        // Bir bolumun ilk gecisinde odenen yildiz coini ust siniri.
        val bolumBasinaYildizCoini = 3 * GameConfig.COINS_PER_STAR

        sablonlar.forEach { sablon ->
            assertTrue(
                "${sablon.id}: gunluk toplam ${sablon.totalRewardCoins} coin — " +
                    "bir bolumun yildiz odulunun ($bolumBasinaYildizCoini) 8 katindan " +
                    "fazla; gunluk gorev oynamaktan karli hale gelir",
                sablon.totalRewardCoins <= bolumBasinaYildizCoini * 8
            )
            assertTrue(
                "${sablon.id}: gunluk toplam pozitif olmali",
                sablon.totalRewardCoins > 0
            )
        }
    }
}
