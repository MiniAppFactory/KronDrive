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
        // -----------------------------------------------------------------
        // OGRENME EGRISI (bolum 1-8) — 2026-08-14'te yeniden tasarlandi
        //
        // Sahibi ikinci kez "ilk bolumler fazla zor" dedi. Ilk turda sadece
        // baslangic hizi dusurulmustu (60/65/70/75 km/h) ama zorlugun ASIL
        // ekseni saniyedeki arac sayisiydi: her bolum tam yogunlukla
        // (0.78 s'de bir arac) basliyordu. Artik yogunluk de rampali
        // ([LevelDef.trafficDensity]).
        //
        // Ogretme sirasi — her bolum TEK yeni sey ogretir:
        //   1 serit degistirme (neredeyse bos yol)  4 perfect dodge
        //   2 trafik                                 5 tam trafik (ilk "normal")
        //   3 boost                                  6 combo (nefes bolumu)
        //   7 baski altinda combo                    8 mesafe + sure
        //
        // Yildiz esikleri docs/BALANCE.md'deki skor egrisinden turetildi:
        //   skor(t) ~ 600 * tabanHiz * (e^(t/54.5) - 1) + 8*gecilenArac + 35*coin
        // ve beklenenin %75-80'ine ayarlandi. Eski bolum 2'nin
        // ScoreAtLeast(2200) hedefi bu egride ~%92'ye denk geliyordu, yani
        // pratikte ulasilamazdi — "fazla zor" sikayetinin somut kaynagi.
        //
        // SIRA ONEMLI: yildizlar SIRALI kazanilir (bkz. LevelEvaluator) ve
        // bir sonraki bolum [GameConfig.MIN_STARS_TO_PASS] gorevle acilir
        // (su an 2). Yani ilk IKI hedef makul olmali; PerfectDodge/Combo gibi
        // beceri hedefleri UCUNCU siraya konur, boylece ustalik yildizi olur
        // ve kimseyi tikamaz. Bu yorum 2026-08-16'da duzeltildi: "en az 1
        // yildiz" yaziyordu ama kural 15 Agustos'ta degismisti ve yorum
        // guncellenmemisti. Eski bolum
        // 3 (`PerfectDodges(3)` ilk sirada) ve bolum 4 (`BoostDistance(500)`
        // ilk sirada) tam olarak bu hatayi yapiyordu.
        // -----------------------------------------------------------------

        // 1 — SADECE SERIT DEGISTIRME. 25 s, 60 km/h, yogunluk 0.30:
        // arac 2.6 s'de bir doguyor, kosu boyunca ~7 arac geliyor. Coin
        // toplamak icin serit degistirmek gerekiyor; kaybetmek neredeyse
        // imkansiz. Uc yildiz da kolay — ilk bolum oyuncuyu odullendirmeli.
        LevelDef(
            id = 1,
            goal = LevelGoal.SurviveTime(25),
            startSpeedKmh = 60,
            trafficDensity = 0.30f,
            stars = listOf(
                Objective.CompleteRun,
                Objective.PassVehicles(3),
                Objective.CoinsAtLeast(3)
            )
        ),
        // 2 — TRAFIK. Yogunluk yariya cikiyor (1.42 s'de bir arac), hiz ve
        // sure ayni rampada. Hedefler hala "oyna, gec" seviyesinde.
        LevelDef(
            id = 2,
            goal = LevelGoal.SurviveTime(30),
            startSpeedKmh = 65,
            trafficDensity = 0.55f,
            stars = listOf(
                Objective.CompleteRun,
                Objective.CoinsAtLeast(6),
                Objective.PassVehicles(14)
            )
        ),
        // 3 — BOOST. BoostDistance(200) tam bir bar dolusu boost'un biraz
        // altinda (dolu bar ~2.6 s x ~35 m/s ~ 90 m; sarjla birlikte kosu
        // boyunca ~450 m yapilabilir), yani "boost'a birkac kez bas" demek.
        LevelDef(
            id = 3,
            goal = LevelGoal.SurviveTime(35),
            startSpeedKmh = 70,
            trafficDensity = 0.70f,
            stars = listOf(
                Objective.PassVehicles(10),
                Objective.BoostDistance(200),
                Objective.ScoreAtLeast(1400)
            )
        ),
        // 4 — PERFECT DODGE. Yogunluk 0.85; uc dodge, mekanigi taniyacak
        // kadar, zorlamayacak kadar.
        LevelDef(
            id = 4,
            goal = LevelGoal.SurviveTime(40),
            startSpeedKmh = 75,
            trafficDensity = 0.85f,
            stars = listOf(
                Objective.CompleteRun,
                Objective.ScoreAtLeast(1800),
                Objective.PerfectDodges(3)
            )
        ),
        // 5 — ILK "NORMAL" BOLUM: varsayilan 80 km/h, tam yogunluk. Buraya
        // gelen oyuncu dort mekanigi de gormus oluyor.
        LevelDef(
            id = 5,
            goal = LevelGoal.SurviveTime(45),
            trafficDensity = 1f,
            stars = listOf(
                Objective.CompleteRun,
                Objective.PassVehicles(30),
                Objective.ScoreAtLeast(2500)
            )
        ),
        // 6 — NEFES BOLUMU + COMBO. Bilerek 5'ten KOLAY (75 km/h, yogunluk
        // 0.85): yeni mekanik once guvenli ortamda ogrenilir. Testere disi
        // egri — bkz. `game-scenario` skill'i.
        LevelDef(
            id = 6,
            goal = LevelGoal.SurviveTime(45),
            startSpeedKmh = 75,
            trafficDensity = 0.85f,
            // 2026-08-16: ikinci hedef PerfectDodges(4) idi, yani bolumun
            // 2. VE 3. hedefi birden beceri hedefiydi. Olcum (LevelCurveTest)
            // temkinli oyunun buradan yalnizca 1 yildiz aldigini gosterdi —
            // [GameConfig.MIN_STARS_TO_PASS] = 2 kuralinda bolum TIKANIYORDU.
            // Combo bu bolumun ogrettigi mekanik, o yuzden ustalik yildizi
            // olarak KALDI; cifte beceri hedefinin ikincisi trafik temasini
            // pekistiren bir gecis hedefiyle degisti.
            stars = listOf(
                Objective.CoinsAtLeast(10),
                Objective.PassVehicles(30),
                Objective.ComboAtLeast(3)
            )
        ),
        // 7 — Ayni combo, bu kez tam trafikte ve daha uzun kosuda.
        LevelDef(
            id = 7,
            goal = LevelGoal.SurviveTime(50),
            // Bolum 6 ile ayni kusur ve ayni duzeltme (2026-08-16):
            // PerfectDodges(6) ikinci siradaydi ve combo ile birlikte iki
            // beceri hedefi yapiyordu. Olcumde temkinli oyun 1 yildiz
            // aliyordu.
            stars = listOf(
                Objective.ScoreAtLeast(2900),
                Objective.PassVehicles(45),
                Objective.ComboAtLeast(4)
            )
        ),
        // --- Mesafe bolumleri: "carpmadan git" baskisi ---
        // 8 — MESAFE. Boost'suz temiz bir kosu 1200 m'yi ~37.5 s'de bitirir
        // (skor egrisinden: 1212 * (e^(t/54.5) - 1) metre). FinishUnder(36)
        // bu yuzden "boost'u kullanmayi ogren" hedefi; 60 s limit ise ilk
        // mesafe bolumunde panik yaratmayacak kadar genis.
        LevelDef(
            id = 8,
            goal = LevelGoal.ReachDistance(meters = 1200, timeLimitSec = 60),
            stars = listOf(
                Objective.CompleteRun,
                Objective.FinishUnderSeconds(36),
                Objective.PerfectDodges(4)
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
