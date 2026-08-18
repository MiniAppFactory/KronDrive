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
        // BASLANGIC HIZI OGRENME BOLUMLERINDE SABIT 60 (2026-08-17, sahibi:
        // "hiz 75'ten degil 60'tan baslasin"). Eskiden 60/65/70/75 diye
        // rampaliydi ve o rampa ilerlemeyi TEK BASINA tasiyordu.
        //
        // Artik tasimasi gerekmiyor: [LevelDef.speedRampScale] ayni gun
        // eklendi ve ilerlemeyi 0.40 -> 0.91 ile kendisi tasiyor. Iki ayri
        // rampa ust uste binince bolum 4 daha ilk saniyeden hizli
        // basliyordu; sahibi bunu oynarken fark etti.
        //
        // Ogretme sirasi — her bolum TEK yeni sey ogretir:
        //   1 serit degistirme (neredeyse bos yol)  4 yogun trafikte skor
        //   2 trafik                                 5 tam trafik (ilk "normal")
        //   3 boost                                  6 combo (nefes bolumu)
        //   7 baski altinda combo                    8 mesafe + sure
        //
        // Yildiz esikleri docs/BALANCE.md'deki skor egrisinden turetildi:
        //   skor(t) ~ 600 * tabanHiz * (e^(t/54.5) - 1) + 8*gecilenArac + 35*coin
        // ve beklenenin %75-80'ine ayarlandi. Eski bolum 2'nin
        // ScoreAtLeast(1600) hedefi bu egride ~%92'ye denk geliyordu, yani
        // pratikte ulasilamazdi — "fazla zor" sikayetinin somut kaynagi.
        //
        // SIRA ONEMLI: yildizlar SIRALI kazanilir (bkz. LevelEvaluator) ve
        // bir sonraki bolum [GameConfig.MIN_STARS_TO_PASS] gorevle acilir
        // (su an 2). Yani ilk IKI hedef makul olmali; beceri hedefleri
        // UCUNCU siraya konur, boylece ustalik yildizi olur ve kimseyi
        // tikamaz.
        //
        // PERFECT DODGE HEDEFLERI KALDIRILDI (2026-08-16, sahibi karari:
        // "dodge hedeflerden kalksin, cok zorlastiriyor cunku"). Iki olcum
        // bunu destekliyor:
        //  - Otuz bolumun tamaminda temkinli oyun **tek bir dodge bile**
        //    yapmiyor (LevelCurveTest, `tam olcum dokumu`). Yani dodge
        //    hedefi olan her bolum temkinli oyuncu icin duvardi.
        //  - Cihazda (40 FPS) dodge penceresi TEK KARE, ~25 ms; insan tepki
        //    tabani ~250 ms (docs/REVIEW_GAMEPLAY.md). Mekanik bir ODUL
        //    olarak duruyor (skor + combo verir) ama artik SART degil.
        // Kaldirmadan once 30 bolumun 14'u ilerlemeyi tikiyordu.
        //
        // COMBO da yalnizca UCUNCU sirada ve en fazla
        // [GameConfig.COMBO_MULTIPLIERS] doyum noktasi kadar (5) istenir —
        // oyun combo 6/7/8'i odullendirmiyor, dolayisiyla istemesi de yanlis.
        // -----------------------------------------------------------------

        // 1 — SADECE SERIT DEGISTIRME. 25 s, 60 km/h, yogunluk 0.30:
        // arac 2.6 s'de bir doguyor, kosu boyunca ~7 arac geliyor. Coin
        // toplamak icin serit degistirmek gerekiyor; kaybetmek neredeyse
        // imkansiz. Uc yildiz da kolay — ilk bolum oyuncuyu odullendirmeli.
        LevelDef(
            id = 1,
            goal = LevelGoal.SurviveTime(25),
            startSpeedKmh = 60,
            speedRampScale = 0.40f,
            trafficDensity = 0.30f,
            stars = listOf(
                Objective.CompleteRun,
                Objective.PassVehicles(2),
                Objective.CoinsAtLeast(3)
            )
        ),
        // 2 — TRAFIK. Yogunluk yariya cikiyor (1.42 s'de bir arac), hiz ve
        // sure ayni rampada. Hedefler hala "oyna, gec" seviyesinde.
        LevelDef(
            id = 2,
            goal = LevelGoal.SurviveTime(30),
            startSpeedKmh = 60,
            speedRampScale = 0.48f,
            trafficDensity = 0.55f,
            stars = listOf(
                Objective.CompleteRun,
                Objective.CoinsAtLeast(6),
                Objective.PassVehicles(8)
            )
        ),
        // 3 — BOOST. BoostDistance(150) tam bir bar dolusu boost'un biraz
        // altinda (dolu bar ~2.6 s x ~35 m/s ~ 90 m; sarjla birlikte kosu
        // boyunca ~450 m yapilabilir), yani "boost'a birkac kez bas" demek.
        LevelDef(
            id = 3,
            goal = LevelGoal.SurviveTime(35),
            startSpeedKmh = 60,
            speedRampScale = 0.56f,
            trafficDensity = 0.70f,
            stars = listOf(
                Objective.PassVehicles(6),
                Objective.BoostDistance(150),
                Objective.ScoreAtLeast(1000)
            )
        ),
        // 4 — YOGUN TRAFIK. Yogunluk 0.85; ilk kez "yol dolu" hissi.
        //
        // 2026-08-16'ya kadar bu bolum PERFECT DODGE bolumuydu ve ucuncu
        // hedefi PerfectDodges(3) idi. Perfect dodge hedefleri KATALOGUN
        // TAMAMINDAN kaldirildi (sahibi karari) — gerekce asagida, listenin
        // basindaki nota bak.
        LevelDef(
            id = 4,
            goal = LevelGoal.SurviveTime(40),
            startSpeedKmh = 60,
            speedRampScale = 0.65f,
            trafficDensity = 0.85f,
            stars = listOf(
                Objective.CompleteRun,
                Objective.ScoreAtLeast(1400),
                Objective.PassVehicles(29)
            )
        ),
        // 5 — ILK "NORMAL" BOLUM: varsayilan 80 km/h, tam yogunluk. Buraya
        // gelen oyuncu dort mekanigi de gormus oluyor.
        LevelDef(
            id = 5,
            goal = LevelGoal.SurviveTime(45),
            startSpeedKmh = 60,
            speedRampScale = 0.74f,
            trafficDensity = 1f,
            stars = listOf(
                Objective.CompleteRun,
                Objective.PassVehicles(18),
                Objective.ScoreAtLeast(1900)
            )
        ),
        // 6 — NEFES BOLUMU + COMBO. Bilerek 5'ten KOLAY (75 km/h, yogunluk
        // 0.85): yeni mekanik once guvenli ortamda ogrenilir. Testere disi
        // egri — bkz. `game-scenario` skill'i.
        LevelDef(
            id = 6,
            goal = LevelGoal.SurviveTime(45),
            startSpeedKmh = 60,
            speedRampScale = 0.82f,
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
                Objective.PassVehicles(18),
                Objective.ComboAtLeast(3)
            )
        ),
        // 7 — Ayni combo, bu kez tam trafikte ve daha uzun kosuda.
        LevelDef(
            id = 7,
            goal = LevelGoal.SurviveTime(50),
            startSpeedKmh = 60,
            speedRampScale = 0.91f,
            // Bolum 6 ile ayni kusur ve ayni duzeltme (2026-08-16):
            // PerfectDodges(6) ikinci siradaydi ve combo ile birlikte iki
            // beceri hedefi yapiyordu. Olcumde temkinli oyun 1 yildiz
            // aliyordu.
            stars = listOf(
                Objective.ScoreAtLeast(2200),
                Objective.PassVehicles(27),
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
            goal = LevelGoal.ReachDistance(meters = 900, timeLimitSec = 60),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.CompleteRun,
                Objective.FinishUnderSeconds(36),
                Objective.CoinsAtLeast(22)
            )
        ),
        LevelDef(
            id = 9,
            goal = LevelGoal.SurviveTime(60),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.CoinsAtLeast(15),
                Objective.PassVehicles(36),
                Objective.ScoreAtLeast(2500)
            )
        ),
        LevelDef(
            id = 10,
            goal = LevelGoal.ReachDistance(meters = 1500, timeLimitSec = 80),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.CompleteRun,
                Objective.PassVehicles(18),
                Objective.FinishUnderSeconds(52)
            )
        ),
        LevelDef(
            id = 11,
            goal = LevelGoal.SurviveTime(70),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.PassVehicles(42),
                Objective.ScoreAtLeast(2900),
                Objective.ComboAtLeast(4)
            )
        ),
        LevelDef(
            id = 12,
            goal = LevelGoal.ReachDistance(meters = 1800, timeLimitSec = 85),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.CompleteRun,
                Objective.CoinsAtLeast(18),
                Objective.FinishUnderSeconds(55)
            )
        ),
        LevelDef(
            id = 13,
            goal = LevelGoal.SurviveTime(70),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.BrakeTapsAtMost(1),
                Objective.CoinsAtLeast(18),
                Objective.ScoreAtLeast(3000)
            )
        ),
        LevelDef(
            id = 14,
            goal = LevelGoal.SurviveTime(75),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.PassVehicles(45),
                Objective.ScoreAtLeast(3300),
                Objective.ComboAtLeast(5)
            )
        ),
        LevelDef(
            id = 15,
            goal = LevelGoal.ReachDistance(meters = 2100, timeLimitSec = 90),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.CompleteRun,
                Objective.BoostDistance(450),
                Objective.FinishUnderSeconds(62)
            )
        ),
        LevelDef(
            id = 16,
            goal = LevelGoal.SurviveTime(75),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.PassVehicles(24),
                Objective.ScoreAtLeast(4100),
                Objective.CoinsAtLeast(48)
            )
        ),
        LevelDef(
            id = 17,
            goal = LevelGoal.SurviveTime(80),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.ScoreAtLeast(3600),
                Objective.PassVehicles(51),
                Objective.ComboAtLeast(5)
            )
        ),
        LevelDef(
            id = 18,
            goal = LevelGoal.ReachDistance(meters = 2400, timeLimitSec = 95),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.CompleteRun,
                Objective.CoinsAtLeast(26),
                Objective.FinishUnderSeconds(66)
            )
        ),
        LevelDef(
            id = 19,
            goal = LevelGoal.SurviveTime(80),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.BrakeTapsAtMost(0),
                Objective.PassVehicles(27),
                Objective.ScoreAtLeast(3800)
            )
        ),
        LevelDef(
            id = 20,
            goal = LevelGoal.SurviveTime(85),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.PassVehicles(54),
                Objective.CoinsAtLeast(24),
                Objective.ComboAtLeast(5)
            )
        ),
        LevelDef(
            id = 21,
            goal = LevelGoal.ReachDistance(meters = 2700, timeLimitSec = 100),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.CompleteRun,
                Objective.BoostDistance(600),
                Objective.FinishUnderSeconds(70)
            )
        ),
        LevelDef(
            id = 22,
            goal = LevelGoal.SurviveTime(85),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.ScoreAtLeast(4000),
                Objective.PassVehicles(54),
                Objective.CoinsAtLeast(54)
            )
        ),
        LevelDef(
            id = 23,
            goal = LevelGoal.SurviveTime(85),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.PassVehicles(54),
                Objective.ScoreAtLeast(4200),
                Objective.ComboAtLeast(5)
            )
        ),
        LevelDef(
            id = 24,
            goal = LevelGoal.ReachDistance(meters = 3000, timeLimitSec = 105),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.CompleteRun,
                Objective.PassVehicles(33),
                Objective.FinishUnderSeconds(74)
            )
        ),
        LevelDef(
            id = 25,
            goal = LevelGoal.SurviveTime(90),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.BrakeTapsAtMost(1),
                Objective.ScoreAtLeast(4400),
                Objective.ComboAtLeast(5)
            )
        ),
        LevelDef(
            id = 26,
            goal = LevelGoal.SurviveTime(90),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.PassVehicles(57),
                Objective.CoinsAtLeast(30),
                Objective.ComboAtLeast(5)
            )
        ),
        LevelDef(
            id = 27,
            goal = LevelGoal.ReachDistance(meters = 3400, timeLimitSec = 115),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.CompleteRun,
                Objective.BoostDistance(750),
                Objective.FinishUnderSeconds(80)
            )
        ),
        LevelDef(
            id = 28,
            goal = LevelGoal.SurviveTime(90),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.ScoreAtLeast(4600),
                Objective.PassVehicles(57),
                Objective.CoinsAtLeast(55)
            )
        ),
        LevelDef(
            id = 29,
            goal = LevelGoal.SurviveTime(90),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.PassVehicles(57),
                Objective.ScoreAtLeast(5000),
                Objective.ComboAtLeast(5)
            )
        ),
        LevelDef(
            id = 30,
            goal = LevelGoal.ReachDistance(meters = 3800, timeLimitSec = 120),
            startSpeedKmh = 60,
            stars = listOf(
                Objective.CompleteRun,
                Objective.PassVehicles(63),
                Objective.FinishUnderSeconds(88)
            )
        )
    )

    val count: Int get() = levels.size

    fun level(id: Int): LevelDef? = levels.firstOrNull { it.id == id }
}
