package com.miniappfactory.krondrive.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCatalogTest {

    @Test
    fun `bolum idleri 1'den count'a kadar bosluksuz ve tekrarsiz`() {
        val ids = LevelCatalog.levels.map { it.id }
        assertEquals(LevelCatalog.count, ids.toSet().size)
        assertEquals((1..LevelCatalog.count).toList(), ids)
    }

    @Test
    fun `her bolumun tam olarak 3 hedefi var`() {
        LevelCatalog.levels.forEach { level ->
            assertEquals("bolum ${level.id}", 3, level.stars.size)
            assertTrue("bolum ${level.id} yildiz vermeli", level.awardsStars)
        }
    }

    @Test
    fun `level id ile dogru bolumu bulur bilinmeyen id null`() {
        LevelCatalog.levels.forEach { level ->
            assertEquals(level, LevelCatalog.level(level.id))
        }
        assertNull(LevelCatalog.level(0))
        assertNull(LevelCatalog.level(LevelCatalog.count + 1))
    }

    @Test
    fun `mesafe bolumlerinde FinishUnderSeconds kendi sure limitinden daha sikidir`() {
        // Aksi halde ucuncu yildiz bedava gelirdi: bolum zaten sure limiti
        // dolmadan bitmek zorunda.
        val distanceLevels = LevelCatalog.levels.filter { it.goal is LevelGoal.ReachDistance }
        assertTrue("en az bir mesafe bolumu olmali", distanceLevels.isNotEmpty())

        distanceLevels.forEach { level ->
            val limit = level.goal.timeLimitSeconds
            level.stars.filterIsInstance<Objective.FinishUnderSeconds>().forEach { objective ->
                assertTrue(
                    "bolum ${level.id}: FinishUnderSeconds(${objective.seconds}) >= limit $limit",
                    objective.seconds < limit
                )
            }
        }
    }

    @Test
    fun `tum hedef sureleri ve mesafeleri pozitif`() {
        LevelCatalog.levels.forEach { level ->
            assertTrue("bolum ${level.id}", level.goal.timeLimitSeconds > 0)
            val goal = level.goal
            if (goal is LevelGoal.ReachDistance) {
                assertTrue("bolum ${level.id}", goal.meters > 0)
            }
        }
    }

    @Test
    fun `sure hedefli bolumlerde FinishUnderSeconds kullanilmaz`() {
        // SurviveTime bolumu her zaman sure dolunca biter; "N saniyenin altinda
        // bitir" hedefi orada mantiken tutturulamaz.
        LevelCatalog.levels
            .filter { it.goal is LevelGoal.SurviveTime }
            .forEach { level ->
                assertTrue(
                    "bolum ${level.id} sure hedefli ama FinishUnderSeconds iceriyor",
                    level.stars.none { it is Objective.FinishUnderSeconds }
                )
            }
    }
}
