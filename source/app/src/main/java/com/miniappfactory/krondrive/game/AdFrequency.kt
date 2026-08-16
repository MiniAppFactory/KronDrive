package com.miniappfactory.krondrive.game

/**
 * Gecis reklami sikligi kararlari. Saf Kotlin — Android bagimliligi yok,
 * boylece JVM testiyle dogrulanabiliyor.
 *
 * Karar iki parcaya ayrilmistir:
 *  - [countsTowardInterstitial]: bu kosu sayaci artirir mi (kosu BITTIGINDE)
 *  - [shouldShow]: sayacin bugunku degeriyle reklam cikar mi (kosudan CIKARKEN)
 *
 * Ayirmanin sebebi 2026-08-16'da bulunan kacak: sayac yalnizca bolum
 * tamamlandiginda artiyordu, dolayisiyla "carpip ana ekrana don -> bolumu
 * tekrar sec" dongusu sinirsiz ve reklamsizdi.
 */
object AdFrequency {

    /**
     * Biten kosu gecis reklami sayacini artirmali mi.
     *
     * Kariyerde basari SARTI YOK — carpan kosu da sayilir. Tek sart kosunun
     * [GameConfig.INTERSTITIAL_MIN_RUN_SECONDS] kadar surmesi: menude yanlis
     * bolume girip hemen cikmak bir oturum sayilmaz.
     *
     * Gunluk gorev kosulari sayaci artirmaz (gunde bir kez oynanir, oradan
     * frekans kurulamaz); sonsuz mod kendi sayacini kullanir.
     */
    fun countsTowardInterstitial(mode: RunMode, stats: RunStats): Boolean = when (mode) {
        RunMode.CAREER -> stats.timeSurvivedSec >= GameConfig.INTERSTITIAL_MIN_RUN_SECONDS
        RunMode.ENDLESS -> true
        RunMode.DAILY -> false
    }

    /**
     * Kosudan cikarken gecis reklami gosterilmeli mi.
     *
     * Ilk [GameConfig.INTERSTITIAL_FREE_LEVELS] bolum reklamsizdir: kacak
     * kapatildiktan sonra en cok tekrar deneyen (= en cok zorlanan) yeni
     * oyuncu en cok reklam goren kesim olmasin diye.
     *
     * @param levelId kariyer bolum numarasi; kariyer disinda null.
     * @param levelsSince kalici sayac (kariyer + gunluk paylasir).
     * @param endlessRunsSince sonsuz modun kalici sayaci.
     */
    fun shouldShow(
        mode: RunMode,
        levelId: Int?,
        levelsSince: Int,
        endlessRunsSince: Int
    ): Boolean = when (mode) {
        RunMode.CAREER, RunMode.DAILY -> {
            val earlyLevel = levelId != null && levelId <= GameConfig.INTERSTITIAL_FREE_LEVELS
            !earlyLevel && levelsSince >= GameConfig.INTERSTITIAL_EVERY_N_LEVELS
        }

        RunMode.ENDLESS ->
            endlessRunsSince >= GameConfig.INTERSTITIAL_EVERY_N_ENDLESS_RUNS
    }
}
