package com.miniappfactory.krondrive.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Haftalik gorevlerin SAYAC BAGLANTISI dogru olmali.
 *
 * 2026-08-19'da bulunan hata: "Boost'la {count} m Git" gorevi
 * [MissionType.DRIVE_DISTANCE] kullaniyordu. Iki sonucu vardi:
 *
 *  1. Baslik yalan soyluyordu — boost'a hic dokunmayan oyuncu gorevi
 *     tamamliyordu.
 *  2. `drive_distance` goreviyle AYNI sayaci paylastigi icin ikisi hep
 *     birlikte doluyordu: iki gorev degil, tek gorev iki kez odeme yapiyordu
 *     (haftada +200 coin ve haftalik sandik).
 *
 * Hicbir test bunu yakalamiyordu cunku her gorev TEK BASINA gecerliydi;
 * kusur gorevler ARASINDAKI iliskideydi. Bu test o iliskiyi kilitler.
 */
class WeeklyMissionWiringTest {

    private val gorevler = WeeklyMissionGenerator.forWeek("2026-W01")

    @Test
    fun `iki gorev ayni sayaci paylasmaz`() {
        val turler = gorevler.map { it.type }
        val tekrar = turler.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(
            "su gorev turleri birden fazla gorevde kullanilmis: $tekrar — " +
                "ayni sayacla beslendikleri icin birlikte dolar, yani tek " +
                "gorev iki kez oder",
            tekrar.isEmpty()
        )
    }

    /**
     * Basligi "Boost" diyen gorev gercekten boost sayacini kullanmali.
     * Kusurun ta kendisi buydu; adiyla mekanigi ayrildiginda kimse fark
     * etmiyor.
     */
    @Test
    fun `boost gorevi boost sayacini kullanir`() {
        val boost = gorevler.first { it.id == "boost_distance" }
        assertEquals(MissionType.BOOST_DISTANCE, boost.type)
        assertTrue(
            "gorev basligi boost demiyor: ${boost.titleTr}",
            boost.titleTr.contains("Boost", ignoreCase = true)
        )
    }

    /** Her gorev turu icin gercekten bir besleme olmali (olu tur birakma). */
    @Test
    fun `kullanilan her tur icin gorev var`() {
        gorevler.forEach { g ->
            assertTrue("${g.id}: kademe yok", g.tiers.isNotEmpty())
            assertTrue(
                "${g.id}: kademeler artan olmali",
                g.tiers.zipWithNext().all { (a, b) -> b.target > a.target }
            )
        }
    }
}
