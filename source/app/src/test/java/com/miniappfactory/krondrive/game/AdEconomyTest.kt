package com.miniappfactory.krondrive.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * REKLAM AKISININ EKONOMIK VE FREKANS SINIRLARI (2026-08-19, QA taramasi).
 *
 * `AdFrequencyTest` KARARI test ediyor: sayac artar mi, reklam cikar mi.
 * Burada test edilen sey karar degil, **kararin dayandigi sabitlerin
 * birbiriyle tutarli olup olmadigi**. Bu ayrimin sebebi 2026-08-16'da
 * bulunan kusur: `AdFrequency.shouldShow` dogru calisiyordu ama gunluk
 * gorevin bolum kimligi (-1) esik karsilastirmasina takiliyor ve gunluk
 * gorev HIC reklam gostermiyordu. Mantik dogru, sabit yanlisti.
 *
 * Odullu reklam tarafinin kalici sayaci ve atomik tahsilati
 * `GameStateRepository` icinde (Context istiyor, JVM'den cagrilamiyor);
 * burada onun DAYANDIGI sinirlar dogrulaniyor.
 */
class AdEconomyTest {

    /**
     * BIR GUNLUK REKLAM GELIRI EKONOMIYI DOMINE ETMEMELI.
     *
     * Garajdaki "coin kazan" reklami ile sonuc ekranindaki "odulu ikiye
     * katla" reklami AYNI gunluk sayaci paylasiyor
     * ([GameStateRepository.grantRewardedCoins]). Bu paylasim bilincli bir
     * karardi: onceden ikiye katlama dogrudan `addCoins` cagiriyordu ve hicbir
     * siniri yoktu, yani ayni kosuyu tekrarlayarak sinirsiz coin basilabiliyordu.
     *
     * Bu test o dersi sayiya baglar: bir gunun tum reklam geliri, katalogdaki
     * en pahali araci ya da bir yukseltme dalinin tamamini ALAMAMALI. Alabilseydi
     * "oyna" degil "reklam izle" oyunun ana ilerleme yolu olurdu.
     */
    @Test
    fun `bir gunluk reklam geliri en pahali icerigi karsilamaz`() {
        val gunlukTavan = GameConfig.REWARDED_COIN_DAILY_LIMIT *
            maxOf(GameConfig.REWARDED_COIN_AMOUNT, GameConfig.REWARDED_DOUBLE_COINS_CAP)

        val enPahaliArac = CarCatalog.shapes.maxOf { it.priceCoins }
        val birDalinTamMaliyeti = (1 until UpgradeCatalog.MAX_LEVEL)
            .sumOf { UpgradeCatalog.cost(it) ?: 0 }

        assertTrue(
            "bir gunluk reklam geliri $gunlukTavan coin — en pahali araci " +
                "($enPahaliArac) tek gunde aliyor",
            gunlukTavan < enPahaliArac
        )
        assertTrue(
            "bir gunluk reklam geliri $gunlukTavan coin — bir yukseltme dalinin " +
                "tamamini ($birDalinTamMaliyeti) tek gunde aliyor",
            gunlukTavan < birDalinTamMaliyeti
        )
    }

    /**
     * "ODULU IKIYE KATLA" TEK BIR REKLAM ODULUNU ASMAMALI.
     *
     * Iki reklam yuzeyi ayni gunluk hakki tuketiyor. Ikiye katlamanin tavani
     * standart odulden buyuk olsaydi, oyuncunun gunluk bes hakkini garajdaki
     * reklamda kullanmasi her zaman zarar olurdu — yani bir yuzey digerini
     * olu hale getirirdi.
     */
    @Test
    fun `ikiye katlama tavani tek reklam odulunu asmaz`() {
        assertTrue(
            "ikiye katlama tavani (${GameConfig.REWARDED_DOUBLE_COINS_CAP}) standart " +
                "odulden (${GameConfig.REWARDED_COIN_AMOUNT}) buyuk — ayni gunluk hakki " +
                "paylasan iki yuzeyden biri anlamsizlasir",
            GameConfig.REWARDED_DOUBLE_COINS_CAP <= GameConfig.REWARDED_COIN_AMOUNT
        )
        assertTrue("odullu reklam gunluk siniri pozitif olmali", GameConfig.REWARDED_COIN_DAILY_LIMIT > 0)
        assertTrue("odullu reklam odulu pozitif olmali", GameConfig.REWARDED_COIN_AMOUNT > 0)
    }

    /**
     * EN KISA KARIYER BOLUMUNU BITIREN KOSU SAYACA GIRMELI.
     *
     * `AdFrequency.countsTowardInterstitial` kariyer kosusunu ancak
     * [GameConfig.INTERSTITIAL_MIN_RUN_SECONDS] kadar surdugunde sayiyor. Esik,
     * en kisa bolumun suresinden buyuk olsaydi o bolumu tekrar tekrar oynayan
     * oyuncunun sayaci HIC ilerlemez ve gecis reklami HIC cikmazdi — hicbir
     * mevcut test bunu gormezdi, cunku hepsi sayaci elle veriyor.
     *
     * Ayni ailenin daha once yasanmis hali: sayac yalnizca bolum tamamlaninca
     * artiyordu ve "carpip cik, tekrar gir" dongusu sinirsiz reklamsizdi
     * (2026-08-16).
     */
    @Test
    fun `en kisa bolumu bitiren kosu gecis reklami sayacini artirir`() {
        LevelCatalog.levels.forEach { bolum ->
            val goal = bolum.goal
            if (goal !is LevelGoal.SurviveTime) return@forEach
            assertTrue(
                "bolum ${bolum.id} ${goal.seconds} saniye suruyor ama reklam sayaci esigi " +
                    "${GameConfig.INTERSTITIAL_MIN_RUN_SECONDS} saniye — bu bolumu " +
                    "tamamlayan kosu sayaca hic girmez",
                goal.seconds >= GameConfig.INTERSTITIAL_MIN_RUN_SECONDS
            )
        }

        // Esigi tam gecen bir kosunun gercekten sayildigini karar fonksiyonuyla dogrula.
        val enKisaSure = LevelCatalog.levels
            .mapNotNull { (it.goal as? LevelGoal.SurviveTime)?.seconds }
            .min()
        assertTrue(
            "en kisa bolumu ($enKisaSure sn) tamamlayan kosu sayilmali",
            AdFrequency.countsTowardInterstitial(
                RunMode.CAREER,
                bosStats(timeSurvivedSec = enKisaSure, completed = true)
            )
        )
    }

    /**
     * MUAFIYET KARIYERI TUKETMEMELI.
     *
     * Ilk [GameConfig.INTERSTITIAL_FREE_LEVELS] bolum reklamsiz. Bu sayi bolum
     * sayisina yaklasirsa kariyerin tamami reklamsiz hale gelir; esik 1 olursa
     * her kosudan sonra reklam cikar ve magaza politikasi tarafinda risk dogar.
     * Iki uc da sessizce olusabilir — ikisi de tek bir sabitin degistirilmesi.
     */
    @Test
    fun `reklam esikleri kariyeri ne tuketir ne bogar`() {
        assertTrue(
            "muafiyet (${GameConfig.INTERSTITIAL_FREE_LEVELS}) kariyerin yarisini " +
                "kapsiyor — kariyer pratikte reklamsiz",
            GameConfig.INTERSTITIAL_FREE_LEVELS < LevelCatalog.levels.size / 2
        )
        assertTrue(
            "gecis reklami esigi ${GameConfig.INTERSTITIAL_EVERY_N_LEVELS} — 2'nin altinda " +
                "olmasi 'her kosudan sonra reklam' demektir",
            GameConfig.INTERSTITIAL_EVERY_N_LEVELS >= 2
        )
        assertFalse(
            "INTERSTITIAL_AFTER_EVERY_RUN acik — her kosudan sonra reklam gosterilir",
            GameConfig.INTERSTITIAL_AFTER_EVERY_RUN
        )
    }

    /**
     * MUAFIYETTEKI SON BOLUM ILE ILK UCRETLI BOLUM SINIRI.
     *
     * `AdFrequency.shouldShow` muafiyeti `levelId in 1..INTERSTITIAL_FREE_LEVELS`
     * ile hesapliyor. Sinirin iki yaninin da davranisi burada kilitleniyor:
     * bir gun karsilastirma `<` ile `<=` arasinda kayarsa test kirilir. Sayac
     * DOLU verilerek muafiyetin gercekten tek belirleyici oldugu gosteriliyor.
     */
    @Test
    fun `muafiyet siniri tam olarak son bedava bolumde biter`() {
        val doluSayac = GameConfig.INTERSTITIAL_EVERY_N_LEVELS

        assertFalse(
            "son bedava bolumde (${GameConfig.INTERSTITIAL_FREE_LEVELS}) reklam cikti",
            AdFrequency.shouldShow(
                mode = RunMode.CAREER,
                levelId = GameConfig.INTERSTITIAL_FREE_LEVELS,
                levelsSince = doluSayac,
                endlessRunsSince = 0
            )
        )
        assertTrue(
            "muafiyetten sonraki ilk bolumde (${GameConfig.INTERSTITIAL_FREE_LEVELS + 1}) " +
                "sayac dolu olmasina ragmen reklam cikmadi",
            AdFrequency.shouldShow(
                mode = RunMode.CAREER,
                levelId = GameConfig.INTERSTITIAL_FREE_LEVELS + 1,
                levelsSince = doluSayac,
                endlessRunsSince = 0
            )
        )
    }

    /**
     * KARIYER SAYACI GUNLUK GOREVLE PAYLASILIYOR — bu bilincli bir karar
     * (`GameStateRepository` ve `KronViewModel.onInterstitialShown`: DAILY de
     * `levelsSince` sayacini sifirliyor). Karar degisirse burada gorunur:
     * gunluk gorev sayaci ARTIRMAZ ama dolu sayacla reklam GOSTERIR.
     */
    @Test
    fun `gunluk gorev sayaci artirmaz ama dolu sayacla reklam gosterir`() {
        assertFalse(
            "gunluk gorev kosusu gecis reklami sayacini artirmamali",
            AdFrequency.countsTowardInterstitial(
                RunMode.DAILY,
                bosStats(timeSurvivedSec = 180, completed = true)
            )
        )
        assertTrue(
            "sayac doluyken gunluk gorev cikisinda reklam gosterilmeli",
            AdFrequency.shouldShow(
                mode = RunMode.DAILY,
                levelId = -1,
                levelsSince = GameConfig.INTERSTITIAL_EVERY_N_LEVELS,
                endlessRunsSince = 0
            )
        )
        assertEquals(
            "gunluk gorevin bolum kimligi sirali bir kariyer numarasi olmamali — " +
                "muafiyet karsilastirmasi ona takiliyordu (2026-08-16)",
            -1, com.miniappfactory.krondrive.data.DailyChallenge.DAILY_LEVEL_ID
        )
    }

    private fun bosStats(timeSurvivedSec: Int, completed: Boolean) = RunStats(
        score = 0,
        timeSurvivedSec = timeSurvivedSec,
        distanceMeters = 0,
        boostDistanceMeters = 0,
        vehiclesPassed = 0,
        perfectDodges = 0,
        bestCombo = 0,
        bigCombos = 0,
        coinsCollected = 0,
        brakeTaps = 0,
        crashed = false,
        revivesUsed = 0,
        completed = completed
    )
}
