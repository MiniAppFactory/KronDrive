package com.miniappfactory.krondrive.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gecis reklami sikligi. Bu testlerin varlik sebebi 2026-08-16'da bulunan
 * kacak: sayac yalnizca bolum TAMAMLANDIYSA artiyordu, yani "carp -> ana
 * ekrana don -> bolumu tekrar sec" dongusu sinirsiz ve reklamsizdi.
 */
class AdFrequencyTest {

    private fun stats(seconds: Int, completed: Boolean) =
        RunStats(timeSurvivedSec = seconds, completed = completed, crashed = !completed)

    // -----------------------------------------------------------------
    // Sayac: hangi kosu sayilir
    // -----------------------------------------------------------------

    @Test
    fun `carpip biten uzun kariyer kosusu sayaci artirir`() {
        // Kacagin ta kendisi: eskiden bu false donuyordu.
        assertTrue(
            AdFrequency.countsTowardInterstitial(
                RunMode.CAREER,
                stats(seconds = 45, completed = false)
            )
        )
    }

    @Test
    fun `tamamlanan kariyer kosusu sayaci artirir`() {
        assertTrue(
            AdFrequency.countsTowardInterstitial(
                RunMode.CAREER,
                stats(seconds = 60, completed = true)
            )
        )
    }

    @Test
    fun `esigin altindaki kisa kosu sayilmaz`() {
        // Yanlis bolume girip hemen cikmak bir oturum degildir.
        assertFalse(
            AdFrequency.countsTowardInterstitial(
                RunMode.CAREER,
                stats(seconds = GameConfig.INTERSTITIAL_MIN_RUN_SECONDS - 1, completed = false)
            )
        )
    }

    @Test
    fun `esige tam oturan kosu sayilir`() {
        assertTrue(
            AdFrequency.countsTowardInterstitial(
                RunMode.CAREER,
                stats(seconds = GameConfig.INTERSTITIAL_MIN_RUN_SECONDS, completed = false)
            )
        )
    }

    @Test
    fun `gunluk gorev kariyer sayacini artirmaz`() {
        // Gunde bir kez oynanir; oradan frekans kurulamaz.
        assertFalse(
            AdFrequency.countsTowardInterstitial(
                RunMode.DAILY,
                stats(seconds = 120, completed = true)
            )
        )
    }

    @Test
    fun `sonsuz mod kosusu her zaman sayilir`() {
        assertTrue(
            AdFrequency.countsTowardInterstitial(
                RunMode.ENDLESS,
                stats(seconds = 3, completed = false)
            )
        )
    }

    // -----------------------------------------------------------------
    // Gosterim: esik ve erken bolum muafiyeti
    // -----------------------------------------------------------------

    private fun show(levelId: Int?, levelsSince: Int, mode: RunMode = RunMode.CAREER) =
        AdFrequency.shouldShow(
            mode = mode,
            levelId = levelId,
            levelsSince = levelsSince,
            endlessRunsSince = 0
        )

    @Test
    fun `ilk bolumlerde sayac dolsa bile reklam cikmaz`() {
        for (levelId in 1..GameConfig.INTERSTITIAL_FREE_LEVELS) {
            assertFalse(
                "bolum $levelId reklamsiz olmali",
                show(levelId = levelId, levelsSince = 99)
            )
        }
    }

    @Test
    fun `muafiyet biter bitmez esik dolduysa reklam cikar`() {
        val firstPaid = GameConfig.INTERSTITIAL_FREE_LEVELS + 1
        assertTrue(
            show(levelId = firstPaid, levelsSince = GameConfig.INTERSTITIAL_EVERY_N_LEVELS)
        )
    }

    @Test
    fun `esik dolmadan reklam cikmaz`() {
        assertFalse(
            show(
                levelId = GameConfig.INTERSTITIAL_FREE_LEVELS + 1,
                levelsSince = GameConfig.INTERSTITIAL_EVERY_N_LEVELS - 1
            )
        )
    }

    @Test
    fun `sonsuz mod kendi esigini kullanir`() {
        assertFalse(
            AdFrequency.shouldShow(
                mode = RunMode.ENDLESS,
                levelId = null,
                levelsSince = 99,
                endlessRunsSince = GameConfig.INTERSTITIAL_EVERY_N_ENDLESS_RUNS - 1
            )
        )
        assertTrue(
            AdFrequency.shouldShow(
                mode = RunMode.ENDLESS,
                levelId = null,
                levelsSince = 0,
                endlessRunsSince = GameConfig.INTERSTITIAL_EVERY_N_ENDLESS_RUNS
            )
        )
    }

    @Test
    fun `gunluk gorevin levelId si yok, muafiyete takilmaz`() {
        assertTrue(
            show(
                levelId = null,
                levelsSince = GameConfig.INTERSTITIAL_EVERY_N_LEVELS,
                mode = RunMode.DAILY
            )
        )
    }

    // -----------------------------------------------------------------
    // Butunluk: kacak gercekten kapandi mi
    // -----------------------------------------------------------------

    @Test
    fun `carpa carpa oynayan oyuncu da reklam gorur`() {
        // Ayni bolumu tekrar tekrar deneyen oyuncuyu simule et: hicbir kosu
        // tamamlanmiyor. Kacak varken bu dongu sonsuza dek reklamsizdi.
        val levelId = GameConfig.INTERSTITIAL_FREE_LEVELS + 1
        var counter = 0
        var adsSeen = 0

        repeat(12) {
            val runStats = stats(seconds = 30, completed = false)
            if (AdFrequency.countsTowardInterstitial(RunMode.CAREER, runStats)) counter++
            if (AdFrequency.shouldShow(RunMode.CAREER, levelId, counter, 0)) {
                adsSeen++
                counter = 0
            }
        }

        assertEquals(12 / GameConfig.INTERSTITIAL_EVERY_N_LEVELS, adsSeen)
    }

    @Test
    fun `esik degerleri makul araliktadir`() {
        // Frekans sessizce "her kosuda reklam"a kaymasin diye alt sinir.
        assertTrue(GameConfig.INTERSTITIAL_EVERY_N_LEVELS >= 2)
        assertTrue(GameConfig.INTERSTITIAL_FREE_LEVELS >= 1)
        assertTrue(GameConfig.INTERSTITIAL_MIN_RUN_SECONDS in 1..30)
    }
}
