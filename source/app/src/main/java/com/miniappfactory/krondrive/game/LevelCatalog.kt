package com.miniappfactory.krondrive.game

/**
 * Kariyer bolumleri — tamamen veri. Yeni bolum eklemek icin listenin sonuna
 * bir [LevelDef] satiri eklemek yeterlidir; motor, kayit ve UI degismez.
 *
 * Hedef degerleri, prototipin gercek skor/hiz egrisine gore hesaplandi
 * (bkz. docs/BALANCE.md): taban hizda ~22 m/s, skor tavanina ulasilinca
 * ~49 m/s; skor kabaca ilk 43 saniyede 1920'ye, 60 saniyede ~4300'e cikar.
 * Bu yuzden urun taslagindaki ornek rakamlar (ornegin "60 saniyede 2000
 * puan") oynanabilirlik icin yukari cekildi.
 */
object LevelCatalog {

    val levels: List<LevelDef> = listOf(
        // --- Ogrenme egrisi: temel kontroller ---
        // Ilk dort bolum daha yavas basliyor (60 -> 75 km/h). Varsayilan 80 km/h
        // ile acilis, oyunu ilk kez acan biri icin fazla hizliydi; skordan gelen
        // hizlanma ayni oldugu icin bolumler yine hizlanarak ilerliyor.
        LevelDef(
            id = 1,
            goal = LevelGoal.SurviveTime(30),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.CompleteRun,
                Objective.PassVehicles(10),
                Objective.CoinsAtLeast(5)
            )
        ),
        LevelDef(
            id = 2,
            goal = LevelGoal.SurviveTime(45),
            startSpeedKmh = 65,
            stars = listOf(
                Objective.PassVehicles(5),
                Objective.ScoreAtLeast(2200),
                Objective.CoinsAtLeast(8)
            )
        ),
        LevelDef(
            id = 3,
            goal = LevelGoal.SurviveTime(45),
            startSpeedKmh = 70,
            stars = listOf(
                Objective.PerfectDodges(3),
                Objective.PerfectDodges(6),
                Objective.ScoreAtLeast(2400)
            )
        ),
        LevelDef(
            id = 4,
            goal = LevelGoal.SurviveTime(60),
            startSpeedKmh = 75,
            stars = listOf(
                Objective.BoostDistance(500),
                Objective.CoinsAtLeast(12),
                Objective.ScoreAtLeast(3000)
            )
        ),
        LevelDef(
            id = 5,
            goal = LevelGoal.SurviveTime(60),
            stars = listOf(
                Objective.BrakeTapsAtMost(2),
                Objective.PassVehicles(20),
                Objective.ScoreAtLeast(3000)
            )
        ),
        LevelDef(
            id = 6,
            goal = LevelGoal.SurviveTime(60),
            stars = listOf(
                Objective.ScoreAtLeast(3200),
                Objective.PerfectDodges(4),
                Objective.CoinsAtLeast(14)
            )
        ),
        LevelDef(
            id = 7,
            goal = LevelGoal.SurviveTime(60),
            stars = listOf(
                Objective.ComboAtLeast(5),
                Objective.PerfectDodges(8),
                Objective.ScoreAtLeast(3200)
            )
        ),
        // --- Mesafe bolumleri: "carpmadan git" baskisi ---
        LevelDef(
            id = 8,
            goal = LevelGoal.ReachDistance(meters = 1500, timeLimitSec = 75),
            stars = listOf(
                Objective.CompleteRun,
                Objective.FinishUnderSeconds(50),
                Objective.PerfectDodges(5)
            )
        ),
        LevelDef(
            id = 9,
            goal = LevelGoal.SurviveTime(60),
            stars = listOf(
                Objective.CoinsAtLeast(15),
                Objective.PerfectDodges(5),
                Objective.ScoreAtLeast(3300)
            )
        ),
        LevelDef(
            id = 10,
            goal = LevelGoal.ReachDistance(meters = 2000, timeLimitSec = 80),
            stars = listOf(
                Objective.CompleteRun,
                Objective.PassVehicles(30),
                Objective.FinishUnderSeconds(52)
            )
        ),
        LevelDef(
            id = 11,
            goal = LevelGoal.SurviveTime(70),
            stars = listOf(
                Objective.PerfectDodges(8),
                Objective.ComboAtLeast(4),
                Objective.ScoreAtLeast(3900)
            )
        ),
        LevelDef(
            id = 12,
            goal = LevelGoal.ReachDistance(meters = 2400, timeLimitSec = 85),
            stars = listOf(
                Objective.CompleteRun,
                Objective.PerfectDodges(3),
                Objective.FinishUnderSeconds(55)
            )
        ),
        LevelDef(
            id = 13,
            goal = LevelGoal.SurviveTime(70),
            stars = listOf(
                Objective.BrakeTapsAtMost(1),
                Objective.CoinsAtLeast(18),
                Objective.ScoreAtLeast(4000)
            )
        ),
        LevelDef(
            id = 14,
            goal = LevelGoal.SurviveTime(75),
            stars = listOf(
                Objective.ComboAtLeast(5),
                Objective.PerfectDodges(10),
                Objective.ScoreAtLeast(4400)
            )
        ),
        LevelDef(
            id = 15,
            goal = LevelGoal.ReachDistance(meters = 2800, timeLimitSec = 90),
            stars = listOf(
                Objective.CompleteRun,
                Objective.BoostDistance(600),
                Objective.FinishUnderSeconds(62)
            )
        ),
        LevelDef(
            id = 16,
            goal = LevelGoal.SurviveTime(75),
            stars = listOf(
                Objective.PassVehicles(40),
                Objective.PerfectDodges(8),
                Objective.CoinsAtLeast(20)
            )
        ),
        LevelDef(
            id = 17,
            goal = LevelGoal.SurviveTime(80),
            stars = listOf(
                Objective.ScoreAtLeast(4800),
                Objective.ComboAtLeast(5),
                Objective.PerfectDodges(12)
            )
        ),
        LevelDef(
            id = 18,
            goal = LevelGoal.ReachDistance(meters = 3200, timeLimitSec = 95),
            stars = listOf(
                Objective.CompleteRun,
                Objective.PerfectDodges(8),
                Objective.FinishUnderSeconds(66)
            )
        ),
        LevelDef(
            id = 19,
            goal = LevelGoal.SurviveTime(80),
            stars = listOf(
                Objective.BrakeTapsAtMost(0),
                Objective.PassVehicles(45),
                Objective.ScoreAtLeast(5000)
            )
        ),
        LevelDef(
            id = 20,
            goal = LevelGoal.SurviveTime(85),
            stars = listOf(
                Objective.PerfectDodges(14),
                Objective.ComboAtLeast(6),
                Objective.CoinsAtLeast(24)
            )
        ),
        LevelDef(
            id = 21,
            goal = LevelGoal.ReachDistance(meters = 3600, timeLimitSec = 100),
            stars = listOf(
                Objective.CompleteRun,
                Objective.BoostDistance(800),
                Objective.FinishUnderSeconds(70)
            )
        ),
        LevelDef(
            id = 22,
            goal = LevelGoal.SurviveTime(85),
            stars = listOf(
                Objective.ScoreAtLeast(5400),
                Objective.PerfectDodges(12),
                Objective.CoinsAtLeast(26)
            )
        ),
        LevelDef(
            id = 23,
            goal = LevelGoal.SurviveTime(85),
            stars = listOf(
                Objective.ComboAtLeast(6),
                Objective.PerfectDodges(16),
                Objective.ScoreAtLeast(5600)
            )
        ),
        LevelDef(
            id = 24,
            goal = LevelGoal.ReachDistance(meters = 4000, timeLimitSec = 105),
            stars = listOf(
                Objective.CompleteRun,
                Objective.PassVehicles(55),
                Objective.FinishUnderSeconds(74)
            )
        ),
        LevelDef(
            id = 25,
            goal = LevelGoal.SurviveTime(90),
            stars = listOf(
                Objective.BrakeTapsAtMost(1),
                Objective.ScoreAtLeast(5800),
                Objective.PerfectDodges(14)
            )
        ),
        LevelDef(
            id = 26,
            goal = LevelGoal.SurviveTime(90),
            stars = listOf(
                Objective.PerfectDodges(18),
                Objective.ComboAtLeast(7),
                Objective.CoinsAtLeast(30)
            )
        ),
        LevelDef(
            id = 27,
            goal = LevelGoal.ReachDistance(meters = 4500, timeLimitSec = 115),
            stars = listOf(
                Objective.CompleteRun,
                Objective.BoostDistance(1000),
                Objective.FinishUnderSeconds(80)
            )
        ),
        LevelDef(
            id = 28,
            goal = LevelGoal.SurviveTime(90),
            stars = listOf(
                Objective.ScoreAtLeast(6200),
                Objective.PerfectDodges(16),
                Objective.CoinsAtLeast(32)
            )
        ),
        LevelDef(
            id = 29,
            goal = LevelGoal.SurviveTime(90),
            stars = listOf(
                Objective.ComboAtLeast(8),
                Objective.PerfectDodges(20),
                Objective.ScoreAtLeast(6600)
            )
        ),
        LevelDef(
            id = 30,
            goal = LevelGoal.ReachDistance(meters = 5000, timeLimitSec = 120),
            stars = listOf(
                Objective.CompleteRun,
                Objective.PerfectDodges(15),
                Objective.FinishUnderSeconds(88)
            )
        )
    )

    val count: Int get() = levels.size

    fun level(id: Int): LevelDef? = levels.firstOrNull { it.id == id }
}
