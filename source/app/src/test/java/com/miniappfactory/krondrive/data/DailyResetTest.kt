package com.miniappfactory.krondrive.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gunluk sayaclarin sifirlanma kurali. Cihaz saatini geri almak eskiden
 * gunluk gorevi ve odullu reklam limitini sifirliyordu; artik sifirlama
 * yalnizca ILERI giden tarihte olur (bkz. GameStateRepository.isFreshDay).
 */
class DailyResetTest {

    @Test
    fun `ilk kurulumda kayitli gun yoktur, sayac taze sayilir`() {
        assertTrue(GameStateRepository.isFreshDay(null, "2026-08-14"))
    }

    @Test
    fun `ayni gun sifirlama yapmaz`() {
        assertFalse(GameStateRepository.isFreshDay("2026-08-14", "2026-08-14"))
    }

    @Test
    fun `ileri giden tarih sifirlar`() {
        assertTrue(GameStateRepository.isFreshDay("2026-08-14", "2026-08-15"))
        assertTrue(GameStateRepository.isFreshDay("2026-08-31", "2026-09-01"))
        assertTrue(GameStateRepository.isFreshDay("2026-12-31", "2027-01-01"))
    }

    @Test
    fun `saat geri alinirsa sayac KORUNUR`() {
        // Exploit: dune donup gunluk gorevi ve reklam limitini tekrar almak.
        assertFalse(GameStateRepository.isFreshDay("2026-08-14", "2026-08-13"))
        assertFalse(GameStateRepository.isFreshDay("2026-09-01", "2026-08-31"))
        assertFalse(GameStateRepository.isFreshDay("2027-01-01", "2026-12-31"))
        // Yillar oncesine donmek de calismaz.
        assertFalse(GameStateRepository.isFreshDay("2026-08-14", "2020-01-01"))
    }

    @Test
    fun `sozluksel siralama gun kimligi formatiyla kronolojik`() {
        // isFreshDay string karsilastirmasina dayaniyor; "yyyy-MM-dd" sifir
        // dolgulu oldugu icin bu gecerli. Dolgu bozulursa bu test duser.
        val days = listOf("2026-01-09", "2026-01-10", "2026-02-01", "2026-10-01", "2027-01-01")
        assertTrue(days.sorted() == days)
        days.zipWithNext { earlier, later ->
            assertTrue(GameStateRepository.isFreshDay(earlier, later))
            assertFalse(GameStateRepository.isFreshDay(later, earlier))
        }
    }

    @Test
    fun `uretilen gun kimligi bu formatta`() {
        val today = DailyChallengeGenerator.currentDayId()
        assertTrue(today, Regex("""\d{4}-\d{2}-\d{2}""").matches(today))
    }
}
