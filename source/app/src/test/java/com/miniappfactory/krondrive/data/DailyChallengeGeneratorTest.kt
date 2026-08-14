package com.miniappfactory.krondrive.data

import com.miniappfactory.krondrive.game.LevelEvaluator
import com.miniappfactory.krondrive.game.RunStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChallengeGeneratorTest {

    private val monthOfDayIds: List<String> = (1..31).map { "2026-08-%02d".format(it) }

    @Test
    fun `ayni gun kimligi her zaman ayni gorevi verir`() {
        monthOfDayIds.forEach { dayId ->
            val first = DailyChallengeGenerator.forDay(dayId)
            repeat(5) { assertEquals(first, DailyChallengeGenerator.forDay(dayId)) }
        }
    }

    @Test
    fun `bir ay icinde birden fazla sablon kullanilir`() {
        val distinct = monthOfDayIds.map { DailyChallengeGenerator.forDay(it).id }.toSet()
        assertTrue("tek sablona takildi: $distinct", distinct.size > 1)
    }

    @Test
    fun `uretilen gorevin LevelDef'i yildiz vermez ve uc kademesi vardir`() {
        monthOfDayIds.forEach { dayId ->
            val challenge = DailyChallengeGenerator.forDay(dayId)
            val def = challenge.toLevelDef()
            assertFalse("$dayId yildiz veriyor", def.awardsStars)
            assertEquals(DailyChallenge.DAILY_LEVEL_ID, def.id)
            assertEquals(DailyChallenge.TIER_COUNT, def.stars.size)
            assertEquals(challenge.tiers.map { it.objective }, def.stars)
            assertEquals(challenge.goal, def.goal)
            // awardsStars = false -> hedef saglansa bile YILDIZ cikmamali;
            // kademeler ayri sayilir (tiersReached).
            assertEquals(0, LevelEvaluator.stars(def, PERFECT_RUN))
            assertEquals(
                DailyChallenge.TIER_COUNT,
                LevelEvaluator.tiersReached(def.stars, PERFECT_RUN)
            )
        }
    }

    @Test
    fun `her gorevin pozitif coin odulu ve gecerli sure limiti var`() {
        monthOfDayIds.forEach { dayId ->
            val challenge = DailyChallengeGenerator.forDay(dayId)
            assertTrue(challenge.totalRewardCoins > 0)
            assertTrue(challenge.goal.timeLimitSeconds > 0)
            assertTrue(challenge.id.isNotBlank())
        }
    }

    @Test
    fun `kademeler artan hedefli ve artan odullu`() {
        monthOfDayIds.forEach { dayId ->
            val challenge = DailyChallengeGenerator.forDay(dayId)
            assertTrue(
                "$dayId: kademe hedefi tanimsiz",
                challenge.tiers.all { it.objective.targetValue != null }
            )
            val targets = challenge.tiers.map { it.objective.targetValue ?: 0 }
            val rewards = challenge.tiers.map { it.rewardCoins }

            assertEquals("$dayId: hedefler artmiyor -> $targets", targets.sorted(), targets)
            assertEquals("$dayId: oduller artmiyor -> $rewards", rewards.sorted(), rewards)
            assertTrue("$dayId: ayni hedef iki kez", targets.toSet().size == targets.size)
            // Kademe hedefleri "yukari sayan" olmali; aksi halde ilk karede
            // saglanmis gorunur ve odul bedava gelirdi.
            assertTrue(
                "$dayId: autoCompletes olmayan hedef var",
                challenge.tiers.all { it.objective.autoCompletes }
            )
        }
    }

    @Test
    fun `ayni kademe ikinci kez odenmez`() {
        val challenge = DailyChallengeGenerator.forDay("2026-08-14")
        val first = challenge.tiers[0].rewardCoins

        // Hic odenmemisken 1. kademe -> sadece o kademenin odulu.
        assertEquals(first, challenge.rewardBetween(alreadyGranted = 0, reached = 1))
        // Bir kademe odenmisken yine 1. kademe -> hicbir sey.
        assertEquals(0, challenge.rewardBetween(alreadyGranted = 1, reached = 1))
        // Bir kademe odenmisken 3. kademe -> sadece 2 ve 3.
        assertEquals(
            challenge.tiers[1].rewardCoins + challenge.tiers[2].rewardCoins,
            challenge.rewardBetween(alreadyGranted = 1, reached = 3)
        )
        assertEquals(challenge.totalRewardCoins, challenge.rewardBetween(0, 3))
    }

    @Test
    fun `currentDayId yyyy-MM-dd formatinda`() {
        val dayId = DailyChallengeGenerator.currentDayId()
        assertTrue(dayId, Regex("""\d{4}-\d{2}-\d{2}""").matches(dayId))
        // Ayni cagride iki kez ayni gun kimligi -> gorev de ayni.
        assertEquals(
            DailyChallengeGenerator.forDay(dayId),
            DailyChallengeGenerator.forDay(DailyChallengeGenerator.currentDayId())
        )
    }

    @Test
    fun `gercek takvim araligindaki tum gun kimlikleri gecerli sablona duser`() {
        // forDay, abs(hashCode) % size kullaniyor; hashCode == Int.MIN_VALUE olan
        // bir girdide abs() negatif kalir ve indeks patlar. Uygulamanin urettigi
        // gun kimlikleri hep "yyyy-MM-dd" formatinda oldugundan burada genis bir
        // takvim araligi taranarak bu riskin pratikte olusmadigi dogrulaniyor.
        for (year in 2020..2035) {
            for (month in 1..12) {
                for (day in 1..31) {
                    val dayId = "%04d-%02d-%02d".format(year, month, day)
                    val challenge = DailyChallengeGenerator.forDay(dayId)
                    assertTrue(dayId, challenge.totalRewardCoins > 0)
                }
            }
        }
    }

    private companion object {
        val PERFECT_RUN = RunStats(
            score = 100_000,
            // Sure kademeli sablon icin de yeterli olsun diye yuksek.
            timeSurvivedSec = 1000,
            distanceMeters = 100_000,
            boostDistanceMeters = 100_000,
            vehiclesPassed = 1000,
            perfectDodges = 1000,
            bestCombo = 100,
            coinsCollected = 1000,
            brakeTaps = 0,
            completed = true
        )
    }
}
