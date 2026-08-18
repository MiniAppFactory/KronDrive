package com.miniappfactory.krondrive.data

import com.miniappfactory.krondrive.game.CarCatalog
import com.miniappfactory.krondrive.game.GameConfig
import com.miniappfactory.krondrive.game.UpgradeLevels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KAYIT BICIMI VE ESKI KAYIT GOCU (2026-08-19, QA bosluk taramasi).
 *
 * `GameStateRepository` bir DataStore Preferences dosyasi kullaniyor ve
 * Map/Set alanlarini **duz metin** olarak sakliyor:
 *
 *     "anahtar:deger,anahtar:deger"
 *
 * Bu bicimin iki ayrilmis karakteri var — virgul ve iki nokta — ve kodun
 * hicbir yerinde kacis (escape) yok. Kodlama/cozme fonksiyonlari
 * (`encodeStringMap`, `decodeStringSet` ...) repository icinde `private`,
 * yani JVM testinden DOGRUDAN cagrilamiyorlar. Bu testler bu yuzden bicimin
 * kendisini degil, **bicime giren SOZLUGU** kilitliyor: kimlikler ayrilmis
 * karakter tasimadigi surece kodlama guvenli.
 *
 * Neden onemli: `decodeStringSet` yalnizca virgulden boler, `decodeStringMap`
 * ise `parts.size == 2` sartini arar. Bir gun katalogda "gece:mavi" gibi bir
 * boya kimligi olusursa oyuncunun **sahip oldugu butun boyalar** bir sonraki
 * okumada sessizce kaybolur — ne bir hata mesaji ne bir cokme olur. Tam
 * olarak "test yesil, urun bozuk" turunden bir kusur.
 */
class SaveMigrationTest {

    /** DataStore metninde ayrilmis olan karakterler. */
    private val ayrilmisKarakterler = listOf(",", ":")

    /**
     * Kayda GIREN her kimlik ayrilmis karakter tasimamali.
     *
     * Kapsam: gövde ve boya kimlikleri (`owned_car_shapes`,
     * `owned_car_colors`, `car_color_by_shape`), guclendirici enum adlari
     * (`owned_boosters`) ve haftalik gorev kimlikleri (`mission_progress`,
     * `mission_claimed`).
     */
    @Test
    fun `kayda giren hicbir kimlik ayrilmis karakter tasimaz`() {
        val kimlikler: List<Pair<String, String>> =
            CarCatalog.shapes.map { "govde" to it.id } +
                CarCatalog.colors.map { "boya" to it.id } +
                BoosterType.entries.map { "guclendirici" to it.name } +
                WeeklyMissionGenerator.forWeek("2026-W01").map { "gorev" to it.id } +
                listOf("haftalik sandik" to WeeklyMissionGenerator.CHEST_CLAIM_KEY)

        kimlikler.forEach { (tur, kimlik) ->
            assertTrue("$tur kimligi bos: '$kimlik'", kimlik.isNotBlank())
            ayrilmisKarakterler.forEach { karakter ->
                assertFalse(
                    "$tur kimligi '$kimlik' ayrilmis '$karakter' karakterini tasiyor — " +
                        "DataStore metni bunun uzerinden bolunuyor, kayit sessizce bozulur",
                    kimlik.contains(karakter)
                )
            }
        }
    }

    /**
     * Haftalik sandik ile gorev kademeleri AYNI kumeyi paylasiyor
     * (`Keys.MISSION_CLAIMED`). Sandigin anahtari bir kademe anahtariyla
     * cakisirsa ya sandik hic acilmaz ya da bir kademe odulu iki kez odenir.
     */
    @Test
    fun `haftalik sandik anahtari hicbir gorev kademesiyle cakismaz`() {
        val gorevler = WeeklyMissionGenerator.forWeek("2026-W01")
        val kademeAnahtarlari = gorevler
            .flatMap { gorev -> gorev.tiers.indices.map { "${gorev.id}#$it" } }
            .toSet()

        assertFalse(
            "haftalik sandik anahtari '${WeeklyMissionGenerator.CHEST_CLAIM_KEY}' bir " +
                "gorev kademesiyle cakisiyor",
            WeeklyMissionGenerator.CHEST_CLAIM_KEY in kademeAnahtarlari
        )
        assertEquals(
            "gorev kademe anahtarlari benzersiz olmali",
            gorevler.sumOf { it.tiers.size },
            kademeAnahtarlari.size
        )
    }

    /**
     * ANAHTARI OLMAYAN ESKI KAYIT.
     *
     * DataStore bir anahtar yoksa `?:` sagindaki varsayilani dondurur, yani
     * [PlayerProgress] varsayilanlari aslinda **eski kayitlarin gordugu
     * degerlerdir**. Bu yuzden varsayilanlarin "sifir/kapali" degil,
     * "guncellemeden onceki davranis" olmasi gerekiyor.
     *
     * Somut ornek (kodda da yazili): `vibrationEnabled` 2026-08-17'de
     * eklendi; titresim ondan once KOSULSUZ calisiyordu. Varsayilan `false`
     * olsaydi guncelleyen her oyuncunun titresimi sessizce kapanirdi.
     */
    @Test
    fun `eksik anahtarli eski kayit oyunu degistirmeden acilir`() {
        val eskiKayit = PlayerProgress()

        assertTrue("ses varsayilan ACIK olmali", eskiKayit.soundEnabled)
        assertTrue(
            "titresim varsayilan ACIK olmali — anahtar eklenmeden once kosulsuz calisiyordu",
            eskiKayit.vibrationEnabled
        )
        assertEquals("ilk bolum her zaman acik olmali", 1, eskiKayit.highestUnlockedLevel)
        assertEquals(
            "yukseltmeler 1'den baslar; 0 olsaydi UpgradeCatalog.cost(0) bedava yukseltme verirdi",
            UpgradeLevels(), eskiKayit.upgrades
        )
        assertEquals("eski kayitta yildiz haritasi bos olmali", 0, eskiKayit.totalStars)
        assertEquals("arac seviyesi hic XP yokken 1", 1, eskiKayit.carLevel)
        assertFalse("dil ekrani bir kez gosterilmeli", eskiKayit.languageChosen)
    }

    /**
     * Varsayilan arac BOS ENVANTERLE de secilebilir olmali.
     *
     * Repository okurken secimi dogruluyor: sahip olunmayan bir kimlik
     * varsayilana duser. Ama varsayilanin KENDISI ucretli olsaydi, hicbir
     * seyi olmayan oyuncunun secimi de dogrulamadan gecemezdi ve garaj
     * onizlemesi ile oyundaki arac birbirinden ayrisirdi.
     */
    @Test
    fun `varsayilan arac ve boya bos envanterle de secili kalir`() {
        val secilenGovde = CarCatalog.selectedShape(CarCatalog.DEFAULT_SHAPE_ID, emptySet())
        assertEquals(
            "varsayilan gövde bos envanterle secili kalmali",
            CarCatalog.DEFAULT_SHAPE_ID, secilenGovde.id
        )

        val acikBoyalar = CarCatalog.effectiveOwnedColors(emptySet(), emptySet())
        assertTrue(
            "varsayilan boya bos envanterle acik olmali, aksi halde varsayilan arac " +
                "kendi renginde cizilemezdi",
            CarCatalog.DEFAULT_COLOR_ID in acikBoyalar
        )
        assertEquals(
            "varsayilan gövdenin fabrika boyasi varsayilan boya olmali",
            CarCatalog.DEFAULT_COLOR_ID, secilenGovde.defaultColorId
        )
    }

    /**
     * BOZUK VEYA ESKIMIS SECIM.
     *
     * Kayitta duran bir kimlik uc sekilde gecersizlesebilir: katalogdan
     * kaldirilmis olabilir, elle bozulmus olabilir ya da oyuncunun HIC SAHIP
     * OLMADIGI ucretli bir icerik olabilir (orn. uygulama verisi kismen
     * silinmis, coin geri gelmis ama envanter gitmis).
     *
     * Ucunde de oyun varsayilana dusmeli — cokmemeli, bos ekran vermemeli.
     */
    @Test
    fun `gecersiz kayitli secim varsayilana duser`() {
        val ucretliGovde = CarCatalog.shapes.first { it.priceCoins > 0 }

        listOf(null, "", "silinmis_arac", ucretliGovde.id).forEach { kayitliId ->
            val secilen = CarCatalog.selectedShape(kayitliId, owned = emptySet())
            assertEquals(
                "kayitli gövde '$kayitliId' bos envanterle varsayilana dusmeliydi",
                CarCatalog.DEFAULT_SHAPE_ID, secilen.id
            )
        }

        // Sahip OLUNAN ucretli gövde ise korunmali; yukaridaki dusus
        // "her seyi varsayilana cevir" anlamina gelmemeli.
        assertEquals(
            "sahip olunan ucretli gövde secili kalmali",
            ucretliGovde.id,
            CarCatalog.selectedShape(ucretliGovde.id, owned = setOf(ucretliGovde.id)).id
        )
    }

    /**
     * Odullu reklamin gunluk sayaci kayitta tutuluyor. Sayac bir sekilde
     * sinirin uzerine cikarsa (surum degisikligiyle sinir DUSURULURSE bu
     * kendiliginden olur) kalan hak NEGATIFE dusmemeli: negatif bir sayi
     * arayuze "-2 hak kaldi" diye yansir.
     */
    @Test
    fun `odullu reklam hakki bozuk sayacla bile negatife dusmez`() {
        val sinir = GameConfig.REWARDED_COIN_DAILY_LIMIT

        assertEquals(
            "hic izlenmemisken tum hak durmali",
            sinir, PlayerProgress(rewardedCoinsToday = 0).rewardedCoinsRemainingToday
        )
        assertEquals(
            "sinira tam oturunca hak bitmeli",
            0, PlayerProgress(rewardedCoinsToday = sinir).rewardedCoinsRemainingToday
        )
        assertEquals(
            "sinirin uzerindeki bozuk sayacta hak 0 olmali, negatif degil",
            0, PlayerProgress(rewardedCoinsToday = sinir + 50).rewardedCoinsRemainingToday
        )
    }

    /**
     * XP kayittan okunur ve arac seviyesi ondan TURETILIR — hem garaj
     * gostergesi hem de seviye sartli arac alimlari bu formule bakiyor
     * (`GameStateRepository.buyCarItem`). Formul her XP degerinde en az 1
     * vermeli ve geriye gitmemeli: seviye dususu, oyuncunun daha once
     * alabildigi bir araci "kilitli" gostermek demektir.
     */
    @Test
    fun `arac seviyesi xp ile hic geriye gitmez ve 1'in altina inmez`() {
        var oncekiSeviye = 0
        var oncekiXp = -1
        val xpDegerleri = listOf(0, 1, GameConfig.XP_PER_CAR_LEVEL - 1) +
            (1..12).map { it * GameConfig.XP_PER_CAR_LEVEL } +
            listOf(Int.MAX_VALUE / 2)

        xpDegerleri.forEach { xp ->
            val seviye = PlayerProgress.carLevelForXp(xp)
            assertTrue("xp=$xp icin arac seviyesi $seviye — 1'in altina inemez", seviye >= 1)
            assertTrue(
                "xp $oncekiXp -> $xp arttigi halde seviye $oncekiSeviye -> $seviye geriledi",
                seviye >= oncekiSeviye
            )
            val ilerleme = PlayerProgress(xp = xp).carLevelProgress
            assertTrue(
                "xp=$xp icin seviye ilerlemesi $ilerleme — 0..1 disinda, cubuk tasar",
                ilerleme in 0f..1f
            )
            assertEquals(
                "PlayerProgress.carLevel ile carLevelForXp ayni formulu kullanmali (xp=$xp)",
                seviye, PlayerProgress(xp = xp).carLevel
            )
            oncekiSeviye = seviye
            oncekiXp = xp
        }
    }
}
