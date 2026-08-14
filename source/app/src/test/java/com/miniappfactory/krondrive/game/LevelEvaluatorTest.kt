package com.miniappfactory.krondrive.game

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelEvaluatorTest {

    private fun level(
        vararg objectives: Objective,
        awardsStars: Boolean = true
    ) = LevelDef(
        id = 1,
        goal = LevelGoal.SurviveTime(30),
        stars = objectives.toList(),
        awardsStars = awardsStars
    )

    private val allMet = RunStats(
        score = 10_000,
        timeSurvivedSec = 20,
        distanceMeters = 5000,
        vehiclesPassed = 100,
        perfectDodges = 50,
        bestCombo = 9,
        coinsCollected = 99,
        completed = true
    )

    @Test
    fun `tamamlanmayan kosu tum hedefler saglansa bile 0 yildiz`() {
        val def = level(
            Objective.PassVehicles(1),
            Objective.PerfectDodges(1),
            Objective.CoinsAtLeast(1)
        )
        assertEquals(0, LevelEvaluator.stars(def, allMet.copy(completed = false)))
    }

    @Test
    fun `tum hedefler saglaninca 3 yildiz`() {
        val def = level(
            Objective.PassVehicles(1),
            Objective.PerfectDodges(1),
            Objective.CoinsAtLeast(1)
        )
        assertEquals(3, LevelEvaluator.stars(def, allMet))
    }

    @Test
    fun `yildizlar sirali kazanilir - ucuncu hedef tek basina sayilmaz`() {
        val def = level(
            Objective.PassVehicles(10),      // saglaniyor
            Objective.PerfectDodges(999),    // saglanmiyor
            Objective.CoinsAtLeast(1)        // saglaniyor ama sirasi gelmiyor
        )
        val stats = allMet.copy(vehiclesPassed = 10, perfectDodges = 0, coinsCollected = 5)
        assertEquals(1, LevelEvaluator.stars(def, stats))
    }

    @Test
    fun `ilk hedef saglanmazsa 0 yildiz`() {
        val def = level(
            Objective.PerfectDodges(999),
            Objective.PassVehicles(1),
            Objective.CoinsAtLeast(1)
        )
        assertEquals(0, LevelEvaluator.stars(def, allMet.copy(perfectDodges = 0)))
    }

    @Test
    fun `awardsStars false olan bolum hic yildiz vermez`() {
        val def = level(
            Objective.PassVehicles(1),
            Objective.PerfectDodges(1),
            Objective.CoinsAtLeast(1),
            awardsStars = false
        )
        assertEquals(0, LevelEvaluator.stars(def, allMet))
    }

    @Test
    fun `bolum yoksa 0 yildiz`() {
        assertEquals(0, LevelEvaluator.stars(null, allMet))
    }

    @Test
    fun `starBreakdown kazanilan yildiz sayisiyla tutarli`() {
        val def = level(
            Objective.PassVehicles(10),
            Objective.PerfectDodges(999),
            Objective.CoinsAtLeast(1)
        )
        val stats = allMet.copy(vehiclesPassed = 10, perfectDodges = 0)
        assertEquals(listOf(true, false, false), LevelEvaluator.starBreakdown(def, stats))
        assertEquals(emptyList<Boolean>(), LevelEvaluator.starBreakdown(null, stats))
    }

    @Test
    fun `FinishUnderSeconds sadece tamamlanmis ve yeterince hizli kosuda saglanir`() {
        val objective = Objective.FinishUnderSeconds(50)
        assertEquals(true, objective.isMet(allMet.copy(timeSurvivedSec = 49, completed = true)))
        assertEquals(false, objective.isMet(allMet.copy(timeSurvivedSec = 51, completed = true)))
        assertEquals(false, objective.isMet(allMet.copy(timeSurvivedSec = 49, completed = false)))
    }
}
