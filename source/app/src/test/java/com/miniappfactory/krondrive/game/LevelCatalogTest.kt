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

    // -----------------------------------------------------------------
    // Ogrenme egrisi (bolum 1-8), 2026-08-14
    // -----------------------------------------------------------------

    @Test
    fun `trafik yogunlugu pozitif ve tam yogunlugu asmaz`() {
        LevelCatalog.levels.forEach { level ->
            assertTrue("bolum ${level.id}", level.trafficDensity > 0f)
            assertTrue(
                "bolum ${level.id}: yogunluk 1'i asamaz (zorluk ekseni hiz ve hedefler)",
                level.trafficDensity <= 1f
            )
        }
    }

    @Test
    fun `ilk bolumler seyrek trafikle basliyor ve kademeli olarak doluyor`() {
        val curve = LevelCatalog.levels.take(5)
        assertTrue(
            "bolum 1 neredeyse bos yol olmali: ${curve[0].trafficDensity}",
            curve[0].trafficDensity <= 0.35f
        )
        curve.zipWithNext().forEach { (a, b) ->
            assertTrue(
                "bolum ${a.id} -> ${b.id}: yogunluk geriye gitmemeli " +
                    "(${a.trafficDensity} -> ${b.trafficDensity})",
                b.trafficDensity >= a.trafficDensity
            )
        }
        assertEquals("bolum 5 ilk tam yogunluk bolumu", 1f, curve.last().trafficDensity, 1e-4f)
    }

    @Test
    fun `ilk bolumler daha yavas basliyor ve hiz geriye gitmiyor`() {
        val speeds = LevelCatalog.levels.take(5).map {
            it.startSpeedKmh ?: GameConfig.speedToKmh(GameConfig.BASE_SPEED) + 1
        }
        assertEquals("bolum 1 en yavas acilis", 60, speeds.first())
        speeds.zipWithNext().forEach { (a, b) ->
            assertTrue("baslangic hizi geriye gitti: $a -> $b", b >= a)
        }
    }

    @Test
    fun `bolum 1 en kisa ve en kolay bolumdur`() {
        val first = LevelCatalog.levels.first()
        LevelCatalog.levels.drop(1).forEach { other ->
            assertTrue(
                "bolum ${other.id} bolum 1'den kisa",
                other.goal.timeLimitSeconds >= first.goal.timeLimitSeconds
            )
            assertTrue(
                "bolum ${other.id} bolum 1'den seyrek",
                other.trafficDensity >= first.trafficDensity
            )
        }
    }

    @Test
    fun `ogrenme bolumlerinde ilk hedef beceri hedefi degildir`() {
        // Yildizlar SIRALI kazanilir ve bir sonraki bolum
        // [GameConfig.MIN_STARS_TO_PASS] gorevle acilir. Ilk hedef
        // PerfectDodge/Combo/BoostDistance gibi bir beceri hedefi olursa
        // oyuncu bolumde tikanir — ilk surumde bolum 3 ve 4 boyleydi.
        LevelCatalog.levels.take(8).forEach { level ->
            val first = level.stars.first()
            assertTrue(
                "bolum ${level.id}: ilk hedef beceri hedefi (${first::class.simpleName})",
                first is Objective.CompleteRun ||
                    first is Objective.PassVehicles ||
                    first is Objective.CoinsAtLeast ||
                    first is Objective.ScoreAtLeast
            )
        }
    }

    @Test
    fun `9 ve sonrasi bolumler ogrenme egrisi degisikliginden etkilenmedi`() {
        // Yogunluk 9+ bolumlerde hala tam (1.0) — ogrenme egrisi yalnizca
        // ilk sekiz bolumu yumusatti.
        LevelCatalog.levels.drop(8).forEach { level ->
            assertEquals("bolum ${level.id} yogunlugu", 1f, level.trafficDensity, 1e-4f)
        }

        // BASLANGIC HIZI ARTIK HER BOLUMDE 60 (2026-08-17, sahibi:
        // "her level'in hizi 60'tan baslasin"). Bu test eskiden 9+ bolumlerin
        // `startSpeedKmh == null` oldugunu donduruyordu; o kural sahibinin
        // karariyla degisti. Yeni kural: hicbir bolum varsayilana DUSMEZ,
        // hepsi acikca 60 yazar — boylece "bu bolum neden hizli basliyor"
        // sorusu katalogda tek bakista cevaplanir.
        LevelCatalog.levels.forEach { level ->
            assertEquals(
                "bolum ${level.id}: her bolum 60 km/h ile baslamali",
                60,
                level.startSpeedKmh
            )
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
