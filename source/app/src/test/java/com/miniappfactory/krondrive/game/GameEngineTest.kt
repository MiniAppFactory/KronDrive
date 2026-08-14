package com.miniappfactory.krondrive.game

import com.miniappfactory.krondrive.data.BoosterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Motor testleri. Iki kural:
 * - Rastgelelik HER ZAMAN sabit tohumlu [Random] ile verilir.
 * - Zaman duvar saatinden degil, sabit dt'li [GameEngine.step] cagrilarindan gelir.
 */
class GameEngineTest {

    private val dt = 0.016f

    private fun engine(
        mode: RunMode = RunMode.CAREER,
        level: LevelDef? = null,
        upgrades: UpgradeLevels = UpgradeLevels(),
        boosters: Set<BoosterType> = emptySet(),
        endlessRecordSeconds: Int = 0,
        seed: Int = 1234
    ): GameEngine = GameEngine(
        mode = mode,
        level = level,
        upgrades = upgrades,
        boosters = boosters,
        endlessRecordSeconds = endlessRecordSeconds,
        random = Random(seed)
    ).apply {
        // Viewport verilmeden serit merkezleri ve oyuncu konumu 0 kalir.
        setViewport(360f, 640f)
    }

    /** Kosu COUNTDOWN ile basliyor; fizik ancak RUNNING'e gecince isliyor. */
    private fun GameEngine.startRun() {
        var guard = 0
        while (phase == RunPhase.COUNTDOWN && guard++ < 1000) step(dt)
        assertEquals(RunPhase.RUNNING, phase)
    }

    /**
     * Kare ilerletir. [clearTraffic] true iken her kareden sonra trafik silinir:
     * carpisma testin konusu degilse kosunun rastgele bir kaza ile bitmesini onler.
     */
    private fun GameEngine.advance(frames: Int, clearTraffic: Boolean = true): List<GameEvent> {
        val all = ArrayList<GameEvent>()
        repeat(frames) {
            all.addAll(step(dt))
            if (clearTraffic) {
                obstacles.clear()
                coins.clear()
                particles.clear()
            }
        }
        return all
    }

    /** Oyuncunun tam ustune bir engel koyar (deterministik carpisma). */
    private fun GameEngine.placeObstacleOnPlayer() {
        obstacles.clear()
        obstacles.add(
            GameEngine.Obstacle(
                lane = playerLane,
                x = playerX,
                y = playerY - 10f,
                colorIndex = 0,
                speedMul = 1f
            )
        )
    }

    private fun threeObjectives() = listOf(
        Objective.CompleteRun,
        Objective.PassVehicles(10_000),
        Objective.CoinsAtLeast(10_000)
    )

    // -----------------------------------------------------------------
    // 1) Hiz sinirlari
    // -----------------------------------------------------------------

    @Test
    fun `hiz MIN_SPEED altina dusmez ve yukseltilmis tavani asmaz`() {
        val upgrades = UpgradeLevels(speed = 4, boost = 3)
        val e = engine(upgrades = upgrades)
        e.startRun()

        val maxSpeed = GameConfig.BASE_SPEED +
            UpgradeCatalog.scoreSpeedCap(upgrades.speed) +
            UpgradeCatalog.boostSpeedBonus(upgrades.boost)

        repeat(4000) { i ->
            e.setBoost(i % 200 < 120)
            e.setBrake(i % 500 < 40)
            e.step(dt)
            e.obstacles.clear()
            e.coins.clear()
            e.particles.clear()
            assertTrue("hiz cok dustu: ${e.speed}", e.speed >= GameConfig.MIN_SPEED - 1e-4f)
            assertTrue("hiz tavani asti: ${e.speed} > $maxSpeed", e.speed <= maxSpeed + 1e-3f)
        }

        // Skor tavani (score / SCORE_SPEED_DIVISOR >= scoreSpeedCap) gercekten
        // asilmis olmali, aksi halde ust sinir hic zorlanmamis olurdu.
        assertTrue(
            "skor tavani zorlanmadi: ${e.score}",
            e.score > UpgradeCatalog.scoreSpeedCap(upgrades.speed) * GameConfig.SCORE_SPEED_DIVISOR
        )
    }

    @Test
    fun `endless carpani hiz tavanini yukari tasir ama sinirli`() {
        val e = engine(RunMode.ENDLESS)
        e.startRun()
        val maxSpeed = (
            GameConfig.BASE_SPEED +
                UpgradeCatalog.scoreSpeedCap(1) +
                UpgradeCatalog.boostSpeedBonus(1)
            ) * GameConfig.ENDLESS_SPEED_MAX_MULTIPLIER
        e.setBoost(true)
        repeat(3000) {
            e.step(dt)
            e.obstacles.clear()
            e.coins.clear()
            e.particles.clear()
            assertTrue(e.speed >= GameConfig.MIN_SPEED - 1e-4f)
            assertTrue("hiz tavani asti: ${e.speed}", e.speed <= maxSpeed + 1e-3f)
        }
    }

    // -----------------------------------------------------------------
    // 2) Boost enerjisi
    // -----------------------------------------------------------------

    @Test
    fun `boost enerjisi basiliyken azalir birakinca dolar ve 0-BOOST_MAX arasinda kalir`() {
        val e = engine()
        e.startRun()
        assertEquals(GameConfig.BOOST_MAX, e.boost, 1e-3f)

        var minSeen = Float.MAX_VALUE
        var maxSeen = -Float.MAX_VALUE
        fun tick(frames: Int) {
            repeat(frames) {
                e.step(dt)
                e.obstacles.clear()
                e.coins.clear()
                e.particles.clear()
                if (e.boost < minSeen) minSeen = e.boost
                if (e.boost > maxSeen) maxSeen = e.boost
            }
        }

        e.setBoost(true)
        tick(60)
        val afterShortBoost = e.boost
        assertTrue("boost harcanmadi: $afterShortBoost", afterShortBoost < GameConfig.BOOST_MAX)

        tick(400) // bari tamamen bosalt
        // Bar bosalinca boost KILITLENIR: parmak basili kaldigi surece ne
        // calisir ne dolar. Titreme (her karede acilip kapanma) boylece yok.
        assertTrue("boost negatif: ${e.boost}", e.boost >= 0f)
        assertTrue("boost bosalmadi: ${e.boost}", e.boost < 1f)
        assertFalse("bos barla boost hala aktif", e.boosting)

        e.setBoost(false)
        tick(60)
        assertTrue("birakinca sarj olmadi: ${e.boost}", e.boost > 1f)

        tick(1500)
        assertEquals(GameConfig.BOOST_MAX, e.boost, 1e-3f)

        assertTrue("boost 0'in altina indi: $minSeen", minSeen >= 0f)
        assertTrue("boost tavani asti: $maxSeen", maxSeen <= GameConfig.BOOST_MAX + 1e-3f)
    }

    // -----------------------------------------------------------------
    // 3) SurviveTime
    // -----------------------------------------------------------------

    @Test
    fun `SurviveTime bolumu sure dolunca completed true ve tek Finished uretir`() {
        val level = LevelDef(id = 1, goal = LevelGoal.SurviveTime(5), stars = threeObjectives())
        val e = engine(RunMode.CAREER, level)
        e.startRun()

        val events = e.advance(600) // 9.6 s > 5 s
        val finished = events.filterIsInstance<GameEvent.Finished>()

        assertEquals(1, finished.size)
        assertEquals(RunPhase.FINISHED, e.phase)
        assertTrue(finished[0].result.stats.completed)
        assertFalse(finished[0].result.stats.crashed)
        // Ilk hedef (CompleteRun) saglandi, ikincisi imkansiz -> tam 1 yildiz.
        assertEquals(1, finished[0].result.stars)
    }

    // -----------------------------------------------------------------
    // 4) ReachDistance
    // -----------------------------------------------------------------

    @Test
    fun `ReachDistance hedef mesafeye ulasinca completed true doner`() {
        val level = LevelDef(
            id = 8,
            goal = LevelGoal.ReachDistance(meters = 300, timeLimitSec = 60),
            stars = threeObjectives()
        )
        val e = engine(RunMode.CAREER, level)
        e.startRun()

        val events = e.advance(1500)
        val finished = events.filterIsInstance<GameEvent.Finished>()

        assertEquals(1, finished.size)
        assertTrue(finished[0].result.stats.completed)
        assertTrue(e.distanceMeters() >= 300)
        assertEquals(RunPhase.FINISHED, e.phase)
    }

    @Test
    fun `ReachDistance sure limiti dolarsa completed false doner`() {
        val level = LevelDef(
            id = 8,
            goal = LevelGoal.ReachDistance(meters = 100_000, timeLimitSec = 4),
            stars = threeObjectives()
        )
        val e = engine(RunMode.CAREER, level)
        e.startRun()

        val events = e.advance(600)
        val finished = events.filterIsInstance<GameEvent.Finished>()

        assertEquals(1, finished.size)
        assertFalse(finished[0].result.stats.completed)
        assertEquals(0, finished[0].result.stars)
    }

    // -----------------------------------------------------------------
    // 5) Carpisma / revive
    // -----------------------------------------------------------------

    @Test
    fun `carpisma CRASHED yapar skor cezasi uygular ve revive bir kez mumkundur`() {
        val e = engine()
        e.startRun()
        e.advance(300) // cezanin olculebilmesi icin skor biriktir

        val scoreBefore = e.score
        assertTrue("test kurgusu: ceza olculebilsin diye skor > ceza olmali", scoreBefore > 100f)

        e.placeObstacleOnPlayer()
        val crashEvents = e.step(dt)

        assertEquals(RunPhase.CRASHED, e.phase)
        assertTrue(crashEvents.any { it is GameEvent.Crash && !it.saved })
        // Ayni karede kucuk bir hiz-skoru da eklendigi icin tolerans birakiliyor.
        assertEquals(scoreBefore - GameConfig.CRASH_SCORE_PENALTY, e.score, 2f)
        assertEquals(0, e.combo)

        assertTrue(e.canRevive())
        e.revive()
        assertEquals(RunPhase.RUNNING, e.phase)
        assertTrue(e.isInvulnerable())

        e.advance(200) // dokunulmazlik (2 sn) bitsin
        assertFalse(e.isInvulnerable())

        e.placeObstacleOnPlayer()
        e.step(dt)
        assertEquals(RunPhase.CRASHED, e.phase)
        // REVIVE_MAX_PER_RUN = 1, ilk hak kullanildi.
        assertFalse(e.canRevive())
    }

    @Test
    fun `carpisip biten kosu completed true istense bile basarisiz sayilir`() {
        val level = LevelDef(id = 1, goal = LevelGoal.SurviveTime(60), stars = threeObjectives())
        val e = engine(RunMode.CAREER, level)
        e.startRun()
        e.advance(60)
        e.placeObstacleOnPlayer()
        e.step(dt)
        assertEquals(RunPhase.CRASHED, e.phase)

        // UI, "devam etme" teklifi reddedilince finish(true) cagirir; motor yine de
        // crashed bayragi yuzunden kosuyu basarisiz sayar.
        e.finish(completed = true)
        val result = e.lastResult!!
        assertFalse(result.stats.completed)
        assertTrue(result.stats.crashed)
        assertEquals(0, result.stars)
    }

    // -----------------------------------------------------------------
    // 6) SECOND_CHANCE booster
    // -----------------------------------------------------------------

    @Test
    fun `SECOND_CHANCE ilk carpismayi yutar ikincisi kosuyu bitirir`() {
        val e = engine(boosters = setOf(BoosterType.SECOND_CHANCE))
        e.startRun()
        e.advance(100)

        e.placeObstacleOnPlayer()
        val first = e.step(dt)
        assertTrue(first.any { it is GameEvent.Crash && it.saved })
        assertEquals(RunPhase.RUNNING, e.phase)
        assertTrue(e.isInvulnerable())

        e.advance(200) // INVULNERABLE_SEC_AFTER_SAVE = 2 sn
        assertFalse(e.isInvulnerable())

        e.placeObstacleOnPlayer()
        val second = e.step(dt)
        assertTrue(second.any { it is GameEvent.Crash && !it.saved })
        assertEquals(RunPhase.CRASHED, e.phase)
    }

    // -----------------------------------------------------------------
    // 7) Perfect dodge
    // -----------------------------------------------------------------

    @Test
    fun `yakin gecen arac perfect dodge sayilir`() {
        val e = engine()
        e.startRun()
        e.advance(60)

        // Esik artik serit araligindan turetiliyor; sabit bir sayi yazmak yerine
        // esigin hemen ALTINA yerlestiriyoruz (carpisma genisliginin de ustunde).
        val dodgeDx = GameConfig.perfectDodgeMaxDx(e.laneWidth) - 2f
        assertTrue("dodge penceresi kapanmis", dodgeDx > GameConfig.CAR_HITBOX_WIDTH_PX)
        e.obstacles.clear()
        e.obstacles.add(
            GameEngine.Obstacle(
                lane = e.playerLane,
                x = e.playerX + dodgeDx,
                y = e.playerY + 10f,
                colorIndex = 0,
                speedMul = 1f
            )
        )

        val events = ArrayList<GameEvent>()
        repeat(30) { events.addAll(e.step(dt)) }

        val dodges = events.filterIsInstance<GameEvent.PerfectDodge>()
        assertEquals(1, dodges.size)
        assertEquals(1, dodges[0].combo)
        assertEquals(1, e.combo)
        assertEquals(RunPhase.RUNNING, e.phase)
    }

    @Test
    fun `bir serit oteden gecen arac perfect dodge sayilmaz`() {
        val e = engine()
        e.startRun()
        e.advance(60)

        // Serit araligi dodge esiginden buyuk olmali; test bunu varsayiyor.
        assertTrue(e.laneWidth > GameConfig.perfectDodgeMaxDx(e.laneWidth))

        e.obstacles.clear()
        e.obstacles.add(
            GameEngine.Obstacle(
                lane = e.playerLane + 1,
                x = e.playerX + e.laneWidth,
                y = e.playerY + 10f,
                colorIndex = 0,
                speedMul = 1f
            )
        )

        val events = ArrayList<GameEvent>()
        repeat(30) { events.addAll(e.step(dt)) }

        assertTrue(events.any { it is GameEvent.VehiclePassed })
        assertTrue(events.none { it is GameEvent.PerfectDodge })
        assertEquals(0, e.combo)
    }

    @Test
    fun `dodge esigi her ekran genisliginde carpisma ile serit araligi arasinda kalir`() {
        // Esik sabit bir piksel degeri OLSAYDI (eski hali: 64), 320 dp'lik
        // telefonlarda serit araligindan (59.7) buyuk kalir ve yan seritten
        // duz gecmek bile bedava dodge verirdi. Bu test o regresyonu yakalar.
        listOf(320f, 360f, 393f, 411f, 480f, 600f, 800f).forEach { width ->
            val e = GameEngine(mode = RunMode.CAREER, level = null, random = Random(1))
            e.setViewport(width, 640f)
            val threshold = GameConfig.perfectDodgeMaxDx(e.laneWidth)
            assertTrue(
                "carpisma siniri asilmali (genislik=$width)",
                threshold > GameConfig.CAR_WIDTH_PX
            )
            assertTrue(
                "temiz serit gecisi dodge saymamali (genislik=$width)",
                threshold < e.laneWidth
            )
        }
    }

    // -----------------------------------------------------------------
    // 8) Sonsuz mod zorluk egrisi
    // -----------------------------------------------------------------

    @Test
    fun `endless hiz carpani 30 saniyelik adimlarla artar ve tavanda durur`() {
        val e = engine(RunMode.ENDLESS)
        e.startRun()
        assertEquals(1f, e.endlessSpeedMultiplier(), 1e-4f)

        e.advance(framesFor(20f))
        assertEquals(1f, e.endlessSpeedMultiplier(), 1e-4f)

        e.advance(framesFor(11f)) // toplam ~31 s
        assertEquals(1f + GameConfig.ENDLESS_SPEED_STEP, e.endlessSpeedMultiplier(), 1e-4f)

        e.advance(framesFor(30f)) // toplam ~61 s
        assertEquals(1f + 2f * GameConfig.ENDLESS_SPEED_STEP, e.endlessSpeedMultiplier(), 1e-4f)

        e.advance(framesFor(140f)) // toplam ~201 s -> 6 adim, tavan
        assertEquals(GameConfig.ENDLESS_SPEED_MAX_MULTIPLIER, e.endlessSpeedMultiplier(), 1e-4f)
    }

    @Test
    fun `kariyer ve gunluk modda hiz carpani her zaman 1`() {
        listOf(RunMode.CAREER, RunMode.DAILY).forEach { mode ->
            val e = engine(mode)
            e.startRun()
            assertEquals(1f, e.endlessSpeedMultiplier(), 1e-4f)
            e.advance(framesFor(65f))
            assertEquals(1f, e.endlessSpeedMultiplier(), 1e-4f)
        }
    }

    private fun framesFor(seconds: Float): Int = (seconds / dt).toInt()

    // -----------------------------------------------------------------
    // 9) DAILY otomatik bitis ve autoCompletes korumasi
    // -----------------------------------------------------------------

    @Test
    fun `DAILY sayan hedef tutturulunca kosu otomatik biter`() {
        val level = LevelDef(
            id = -1,
            goal = LevelGoal.SurviveTime(180),
            stars = listOf(Objective.DistanceAtLeast(100)),
            awardsStars = false
        )
        val e = engine(RunMode.DAILY, level)
        e.startRun()

        val events = e.advance(600)
        val finished = events.filterIsInstance<GameEvent.Finished>()

        assertEquals(1, finished.size)
        assertEquals(RunPhase.FINISHED, e.phase)
        assertTrue(finished[0].result.stats.completed)
        assertTrue(e.distanceMeters() >= 100)
        // awardsStars = false oldugu icin yildiz yok.
        assertEquals(0, finished[0].result.stars)
    }

    @Test
    fun `DAILY ilk kademede bitmez son kademede biter`() {
        val level = LevelDef(
            id = -1,
            goal = LevelGoal.SurviveTime(180),
            stars = listOf(
                Objective.DistanceAtLeast(100),
                Objective.DistanceAtLeast(400),
                Objective.DistanceAtLeast(900)
            ),
            awardsStars = false
        )
        val e = engine(RunMode.DAILY, level)
        e.startRun()

        // Ilk kademe gecildi ama kosu SURMELI — ustteki kademeler ayni
        // kosuda toplanabilsin diye.
        while (e.distanceMeters() < 150) e.advance(1)
        assertEquals(RunPhase.RUNNING, e.phase)

        val finished = e.advance(3000).filterIsInstance<GameEvent.Finished>()
        assertEquals(1, finished.size)
        assertEquals(3, finished[0].result.dailyTiers)
        assertTrue(e.distanceMeters() >= 900)
    }

    @Test
    fun `DAILY carpisma kademeleri silmez`() {
        val level = LevelDef(
            id = -1,
            goal = LevelGoal.SurviveTime(180),
            stars = listOf(
                Objective.DistanceAtLeast(100),
                Objective.DistanceAtLeast(400),
                Objective.DistanceAtLeast(100_000)
            ),
            awardsStars = false
        )
        val e = engine(RunMode.DAILY, level)
        e.startRun()

        while (e.distanceMeters() < 450) e.advance(1)
        e.placeObstacleOnPlayer()
        e.step(dt)
        assertEquals(RunPhase.CRASHED, e.phase)

        e.finish(completed = false)
        val result = requireNonNull(e.lastResult)
        // Kosu basarisiz bitti (completed = false) ama tutturulmus iki
        // kademe sayilir — yoksa son saniyede carpan oyuncu her seyi kaybederdi.
        assertFalse(result.stats.completed)
        assertEquals(2, result.dailyTiers)
    }

    @Test
    fun `kariyer ve sonsuz modda dailyTiers her zaman sifir`() {
        val level = LevelDef(
            id = 3,
            goal = LevelGoal.SurviveTime(2),
            stars = threeObjectives()
        )
        val career = engine(RunMode.CAREER, level)
        career.startRun()
        career.advance(400)
        assertEquals(0, requireNonNull(career.lastResult).dailyTiers)

        val endless = engine(RunMode.ENDLESS)
        endless.startRun()
        endless.advance(60)
        endless.finish(completed = false)
        assertEquals(0, requireNonNull(endless.lastResult).dailyTiers)
    }

    // -----------------------------------------------------------------
    // 11) Farm engelleri (2026-08-14)
    // -----------------------------------------------------------------

    @Test
    fun `yildiz coini sadece YENI yildiz icin odenir`() {
        // Ayni bolumu tekrar oynamak eskiden yildiz coinini yeniden odiyordu:
        // bolum 1'i (kisa, kolay uc yildiz) farmlamak oyunun en karli
        // dongusuydu. Artik ikinci gecis 0 yildiz coini oder.
        fun run(previousStars: Int): RunResult {
            val level = LevelDef(
                id = 3,
                goal = LevelGoal.SurviveTime(12),
                stars = listOf(
                    Objective.CompleteRun,
                    Objective.DistanceAtLeast(1),
                    Objective.PassVehicles(0)
                )
            )
            val e = GameEngine(
                mode = RunMode.CAREER,
                level = level,
                endlessRecordSeconds = 0,
                previousStars = previousStars,
                random = Random(7)
            ).apply { setViewport(360f, 640f) }
            e.startRun()
            e.advance(2000)
            return requireNonNull(e.lastResult)
        }

        val first = run(previousStars = 0)
        assertEquals(3, first.stars)
        assertEquals(3, first.newStars)

        val repeat = run(previousStars = 3)
        assertEquals("yildiz sayisi ayni kalmali", 3, repeat.stars)
        assertEquals("ikinci gecis yeni yildiz vermez", 0, repeat.newStars)
        assertTrue(
            "tekrar oynayis daha az coin odemeli: ${first.coinsEarned} -> ${repeat.coinsEarned}",
            repeat.coinsEarned < first.coinsEarned
        )
        assertEquals(
            first.coinsEarned - 3 * GameConfig.COINS_PER_STAR,
            repeat.coinsEarned
        )
    }

    @Test
    fun `cok kisa kosu hic coin odemez`() {
        val e = engine(RunMode.ENDLESS)
        e.startRun()
        // MIN_PAID_RUN_SECONDS'in altinda kalacak kadar kisa bir kosu.
        e.advance(framesFor(GameConfig.MIN_PAID_RUN_SECONDS - 2f))
        e.finish(completed = false)

        val short = requireNonNull(e.lastResult)
        assertTrue(short.stats.timeSurvivedSec < GameConfig.MIN_PAID_RUN_SECONDS)
        assertEquals("kisa kosu coin odememeli", 0, short.coinsEarned)

        // Esigin ustunde ayni kosu oduyor — kural sadece cok kisalari kesiyor.
        val long = engine(RunMode.ENDLESS)
        long.startRun()
        long.advance(framesFor(GameConfig.MIN_PAID_RUN_SECONDS + 6f))
        long.finish(completed = false)
        assertTrue(requireNonNull(long.lastResult).coinsEarned > 0)
    }

    private fun requireNonNull(result: RunResult?): RunResult {
        assertTrue("kosu sonucu olusmadi", result != null)
        return result!!
    }

    // -----------------------------------------------------------------
    // 10) Hissedilen hiz olcegi (GameConfig.WORLD_SPEED_SCALE)
    // -----------------------------------------------------------------

    @Test
    fun `dunya olcegi gostergeyi ve metre karsiligini degistirmez`() {
        // Gosterge yalnizca speed'den hesaplanir: olcek onu ETKILEMEMELI.
        // Taban hiz prototipteki gibi 79-80 km/h gosterir (tam deger 79.89).
        assertEquals(79, GameConfig.speedToKmh(GameConfig.BASE_SPEED))
        assertEquals(GameConfig.SPEEDOMETER_MAX_KMH.toInt(), GameConfig.speedToKmh(100f))

        // Metre/saniye olcekten BAGIMSIZ olmali: piksel de metre de ayni
        // carpanla kuculdugu icin oran sadelesir. Tutmazsa tum bolumlerin
        // mesafe hedefleri sessizce uzar veya kisalirdi.
        listOf(GameConfig.MIN_SPEED, GameConfig.BASE_SPEED, 4f, 5.17f, 6.5f).forEach { speed ->
            val prototype = speed * GameConfig.WORLD_PX_PER_SPEED_UNIT_PROTOTYPE /
                GameConfig.PIXELS_PER_METER_PROTOTYPE
            val scaled = speed * GameConfig.WORLD_PX_PER_SPEED_UNIT / GameConfig.PIXELS_PER_METER
            assertEquals("$speed birim", prototype, scaled, 0.01f)
        }

        // Taban hizin metre karsiligi prototipteki gibi ~22.2 m/s (80 km/h).
        assertEquals(
            22.2f,
            GameConfig.BASE_SPEED * GameConfig.WORLD_PX_PER_SPEED_UNIT / GameConfig.PIXELS_PER_METER,
            0.2f
        )
    }

    @Test
    fun `dunya olcegi yol kaymasini yavaslatir`() {
        // Ayni hizda dunyanin kaydigi piksel, prototipin tam olcek karsiligidir.
        val e = engine()
        e.startRun()
        val before = e.roadOffset
        val speedBefore = e.speed
        e.step(dt)
        val travelled = e.roadOffset - before

        val expected = speedBefore * GameConfig.WORLD_PX_PER_SPEED_UNIT_PROTOTYPE *
            GameConfig.WORLD_SPEED_SCALE * dt
        assertEquals(expected, travelled, expected * 0.05f)
        assertTrue(
            "olcek 1.0 ise bu test bir sey dogrulamiyor",
            GameConfig.WORLD_SPEED_SCALE < 1f
        )
    }

    @Test
    fun `DAILY CompleteRun hedefi kosunun basinda otomatik bitirmez`() {
        val level = LevelDef(
            id = -1,
            goal = LevelGoal.SurviveTime(90),
            stars = listOf(Objective.CompleteRun),
            awardsStars = false
        )
        val e = engine(RunMode.DAILY, level)
        e.startRun()
        e.advance(120)

        assertEquals(RunPhase.RUNNING, e.phase)
        assertNull(e.lastResult)
    }

    @Test
    fun `DAILY BrakeTapsAtMost hedefi kosunun basinda otomatik bitirmez`() {
        val level = LevelDef(
            id = -1,
            goal = LevelGoal.SurviveTime(90),
            stars = listOf(Objective.BrakeTapsAtMost(3)),
            awardsStars = false
        )
        val e = engine(RunMode.DAILY, level)
        e.startRun()
        e.advance(120)

        assertEquals(RunPhase.RUNNING, e.phase)
        assertNull(e.lastResult)
    }

    // -----------------------------------------------------------------
    // Akis: duraklat / devam et
    // -----------------------------------------------------------------

    @Test
    fun `pause fizigi dondurur resume kaldigi yerden devam eder`() {
        val e = engine()
        e.startRun()
        e.advance(60)

        val distanceBefore = e.distanceMeters()
        e.pause()
        assertEquals(RunPhase.PAUSED, e.phase)
        e.advance(60)
        assertEquals(distanceBefore, e.distanceMeters())

        e.resume()
        assertEquals(RunPhase.RUNNING, e.phase)
        e.advance(60)
        assertTrue(e.distanceMeters() > distanceBefore)
    }

    @Test
    fun `geri sayim bitmeden fizik islemez`() {
        val e = engine()
        e.step(dt)
        assertEquals(RunPhase.COUNTDOWN, e.phase)
        assertEquals(0, e.distanceMeters())
        assertEquals(0f, e.score, 1e-4f)
        assertTrue(e.countdownRemaining < GameConfig.COUNTDOWN_SECONDS.toFloat())
    }

    @Test
    fun `ayni tohum ayni kosuyu uretir`() {
        fun run(): Triple<Int, Int, Float> {
            val e = engine(seed = 99)
            e.startRun()
            repeat(500) { i ->
                e.setBoost(i % 100 < 50)
                e.step(dt)
            }
            return Triple(e.distanceMeters(), e.obstacles.size, e.score)
        }
        assertEquals(run(), run())
    }
}
