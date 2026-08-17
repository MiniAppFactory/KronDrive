package com.miniappfactory.krondrive.game

import com.miniappfactory.krondrive.data.BoosterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
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
        seed: Int = 1234,
        shape: CarShapeDef = CarCatalog.defaultShape
    ): GameEngine = GameEngine(
        mode = mode,
        level = level,
        upgrades = upgrades,
        boosters = boosters,
        endlessRecordSeconds = endlessRecordSeconds,
        carStyle = CarStyle(shape, CarCatalog.defaultColor),
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
                ownSpeed = 0f
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
                ownSpeed = 0f
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
                ownSpeed = 0f
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
    fun `boost bosalinca hazir degil isaretlenir ve birakilinca geri gelir`() {
        // Oyuncu "boost yaptim ama saymadi" dedi: bar bosaliyor, parmak
        // basili kaldigi icin kilit aciliyor ve boost bir daha tutusmuyor.
        // Motor dogru davraniyordu; eksik olan bu bayrakti (arayuz bununla
        // butonu soluklastiriyor).
        val e = engine()
        e.startRun()
        assertTrue("kosu basinda boost hazir olmali", e.isBoostReady())

        e.setBoost(true)
        e.advance(framesFor(6f))          // bar bosalir
        assertFalse("bar bosaldi, boost hazir gorunmemeli", e.isBoostReady())

        // Parmak basili kaldigi surece dolmaz.
        e.advance(framesFor(4f))
        assertFalse(e.isBoostReady())

        // Birakinca dolar ve yeniden hazir olur.
        e.setBoost(false)
        e.advance(framesFor(4f))
        assertTrue("birakildiktan sonra boost geri gelmeli", e.isBoostReady())
    }

    @Test
    fun `hiz kilidi yalnizca sonsuz modda ve hizi sabitler`() {
        // Kariyer ve gunluk gorevde kilit hic acilmamali.
        listOf(RunMode.CAREER, RunMode.DAILY).forEach { mode ->
            val e = engine(mode)
            assertFalse(e.canLockSpeed())
            e.toggleSpeedLock()
            assertFalse("$mode modunda kilit acilmamali", e.speedLocked)
        }

        val e = engine(RunMode.ENDLESS)
        assertTrue(e.canLockSpeed())
        e.startRun()
        e.advance(framesFor(20f))

        val beforeLock = e.speed
        e.toggleSpeedLock()
        assertTrue(e.speedLocked)

        // Kilitliyken skor birikmeye devam etse de hiz buyumemeli.
        e.advance(framesFor(40f))
        assertEquals("kilitli hiz sabit kalmali", beforeLock, e.speed, 0.05f)

        // Kilit acilinca skordan gelen hizlanma geri gelir.
        e.toggleSpeedLock()
        assertFalse(e.speedLocked)
        e.advance(framesFor(15f))
        assertTrue("kilit acilinca hiz yeniden artmali", e.speed > beforeLock + 0.1f)
    }

    @Test
    fun `KARIYER tum hedefler tutunca bolum hemen biter`() {
        // Oyuncu uc hedefi de tuttugunda bekletilmemeli (2026-08-15).
        val level = LevelDef(
            id = 3,
            goal = LevelGoal.SurviveTime(180),
            stars = listOf(
                Objective.DistanceAtLeast(80),
                Objective.DistanceAtLeast(160),
                Objective.DistanceAtLeast(240)
            )
        )
        val e = engine(RunMode.CAREER, level)
        e.startRun()

        val finished = e.advance(3000).filterIsInstance<GameEvent.Finished>()
        assertEquals(1, finished.size)
        val result = finished[0].result
        assertTrue("hedefler tutunca basarili sayilmali", result.stats.completed)
        assertEquals("uc hedef de tuttuysa uc yildiz", 3, result.stars)
        // Sure limiti 180 sn; bolum çok daha erken bitmis olmali.
        assertTrue("bolum sure limitini beklememeli", result.stats.timeSurvivedSec < 60)
    }

    @Test
    fun `KARIYER tamamlama hedefi varsa erken bitmez`() {
        // CompleteRun kosu bitmeden degerlendirilemez; boyle bir hedef varsa
        // bolum yine kendi sure/mesafe hedefiyle biter.
        val level = LevelDef(
            id = 1,
            goal = LevelGoal.SurviveTime(8),
            stars = listOf(
                Objective.CompleteRun,
                Objective.DistanceAtLeast(10),
                Objective.DistanceAtLeast(20)
            )
        )
        val e = engine(RunMode.CAREER, level)
        e.startRun()
        e.advance(120)
        assertEquals("erken bitmemeli", RunPhase.RUNNING, e.phase)

        val finished = e.advance(3000).filterIsInstance<GameEvent.Finished>()
        assertEquals(1, finished.size)
        assertTrue(finished[0].result.stats.completed)
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

    @Test
    fun `revive sonrasi yol bos ve bir sure yeni arac dogmuyor`() {
        val e = engine()
        e.startRun()
        // Ekranin USTUNDE bekleyen bir arac: eski kod bunu silmiyordu ve
        // dokunulmazlik biter bitmez oyuncunun uzerine iniyordu.
        e.obstacles.add(
            GameEngine.Obstacle(
                lane = e.playerLane,
                x = e.playerX,
                y = -GameConfig.CAR_HEIGHT_PX - 40f,
                colorIndex = 0,
                ownSpeed = 0f
            )
        )
        e.placeObstacleOnPlayer()
        e.step(dt)
        assertEquals(RunPhase.CRASHED, e.phase)

        e.revive()
        assertEquals(RunPhase.RUNNING, e.phase)
        assertTrue("revive yolu tamamen temizlemeli", e.obstacles.isEmpty())
        assertTrue(e.isInvulnerable())

        // Guvenli pencere boyunca hic arac dogmamali (trafik temizlenmeden
        // ilerletiyoruz — advance(clearTraffic = false)).
        val pauseFrames = (GameConfig.REVIVE_SPAWN_PAUSE_SEC / dt).toInt() - 2
        e.advance(pauseFrames, clearTraffic = false)
        assertTrue(
            "guvenli pencerede arac dogdu: ${e.obstacles.size}",
            e.obstacles.isEmpty()
        )
        assertTrue("dokunulmazlik pencere boyunca surmeli", e.isInvulnerable())

        // Pencere bitince trafik geri gelir.
        e.advance((1.2f / dt).toInt(), clearTraffic = false)
        assertTrue("pencere bitti, trafik geri gelmeli", e.obstacles.isNotEmpty())
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
    // 12) Trafik kendi hiziyla ilerliyor (2026-08-14)
    //
    // Sorun: engeller `speed * K * dt * speedMul` (1.00..1.14) ile
    // akiyordu, zemin ise tam `speed * K * dt` — yani araclar asfalta gore
    // duruyordu ("park etmis gibiler"). Artik her aracin kendi hizi var.
    // -----------------------------------------------------------------

    /** Bir karede tek bir engelin ekranda kac piksel asagi aktigini olcer. */
    private fun GameEngine.measureApproachPx(ownSpeed: Float): Float {
        obstacles.clear()
        val o = GameEngine.Obstacle(
            lane = 0,
            x = laneCenter(0),
            y = -100f,
            colorIndex = 0,
            ownSpeed = ownSpeed
        )
        obstacles.add(o)
        val before = o.y
        step(dt)
        return o.y - before
    }

    @Test
    fun `trafik araclari asfalta gore ILERI gider`() {
        val e = engine()
        e.startRun()
        e.advance(30)

        val ownSpeed = GameConfig.BASE_SPEED * GameConfig.TRAFFIC_SPEED_RATIO_MIN
        val roadBefore = e.roadOffset
        val approach = e.measureApproachPx(ownSpeed)
        val roadTravel = e.roadOffset - roadBefore

        // Isaret testi: arac hala oyuncuya yaklasiyor (asagi akiyor) ...
        assertTrue("arac oyuncuya yaklasmiyor: $approach", approach > 0f)
        // ... ama zeminden DAHA YAVAS akiyor, yani asfalta gore ILERI gidiyor.
        // Eski modelde bu fark negatifti (speedMul >= 1.0).
        assertTrue(
            "arac asfaltla ayni ya da daha hizli akiyor: $approach vs zemin $roadTravel",
            approach < roadTravel
        )
        // Asfalta gore ileri hizi tam olarak aracin kendi hizidir.
        val relativeToRoad = roadTravel - approach
        assertEquals(
            ownSpeed * GameConfig.WORLD_PX_PER_SPEED_UNIT * dt,
            relativeToRoad,
            0.5f
        )
    }

    @Test
    fun `park etmis arac (ownSpeed 0) asfaltla ayni hizda akar`() {
        // Eski davranisin ta kendisi — testlerin deterministik engelleri bunu
        // kullaniyor. Kontrol grubu olarak duruyor.
        val e = engine()
        e.startRun()
        e.advance(30)
        val roadBefore = e.roadOffset
        val approach = e.measureApproachPx(ownSpeed = 0f)
        assertEquals(e.roadOffset - roadBefore, approach, 0.5f)
    }

    @Test
    fun `boost yaklasma hizini oransal olarak dunyadan daha cok artirir`() {
        // Trafigin hizi kosunun TABAN hizina bagli (anlik hiza degil), bu
        // yuzden boost'un +1.8 birimi dogrudan yaklasma hizina eklenir.
        // Eski modelde trafik oyuncunun anlik hiziyla olceklendigi icin
        // boost yaklasma hizini yalnizca dunya kadar artiriyordu.
        val ownSpeed = GameConfig.BASE_SPEED * GameConfig.TRAFFIC_SPEED_RATIO_MAX

        val calm = engine()
        calm.startRun()
        calm.advance(30)
        val calmRoadBefore = calm.roadOffset
        val calmApproach = calm.measureApproachPx(ownSpeed)
        val calmRoad = calm.roadOffset - calmRoadBefore

        val boosted = engine()
        boosted.startRun()
        boosted.setBoost(true)
        boosted.advance(30)
        val boostRoadBefore = boosted.roadOffset
        val boostApproach = boosted.measureApproachPx(ownSpeed)
        val boostRoad = boosted.roadOffset - boostRoadBefore

        assertTrue("boost aktif degil", boosted.boosting)
        assertTrue("boost hizlandirmadi", boosted.speed > calm.speed)
        assertTrue("boost yaklasmayi artirmadi", boostApproach > calmApproach)
        assertTrue(
            "boost yaklasma hizini dunyadan daha cok artirmali: " +
                "yaklasma x${boostApproach / calmApproach}, dunya x${boostRoad / calmRoad}",
            boostApproach / calmApproach > boostRoad / calmRoad + 0.05f
        )
    }

    @Test
    fun `ekranin ustunden cikan arac temizlenir ve puan vermez`() {
        // Oyuncu frene basip trafigin ALTINA duserse yaklasma hizi negatife
        // doner ve arac yukari uzaklasir. Eskiden sadece alt sinir vardi;
        // boyle bir arac listede sonsuza kadar kalirdi.
        val e = engine()
        e.startRun()
        e.advance(30)

        e.obstacles.clear()
        e.obstacles.add(
            GameEngine.Obstacle(
                lane = 0,
                x = e.laneCenter(0),
                y = -100f,
                colorIndex = 0,
                // Oyuncudan cok daha hizli: yaklasma hizi negatif.
                ownSpeed = e.speed * 3f
            )
        )

        val events = ArrayList<GameEvent>()
        var guard = 0
        while (e.obstacles.isNotEmpty() && guard++ < 600) {
            events.addAll(e.step(dt))
            // Dogan yeni araclari saymayalim; sadece bizimkini izliyoruz.
            e.obstacles.removeAll { it.ownSpeed < e.speed }
        }

        assertTrue("yukari giden arac temizlenmedi", e.obstacles.isEmpty())
        assertTrue(
            "gecilmeyen arac puan verdi",
            events.none { it is GameEvent.VehiclePassed }
        )
    }

    @Test
    fun `ayni arac iki kez sayilmaz`() {
        // Yaklasma hizi isaret degistirirse (fren) gecilmis bir arac yeniden
        // oyuncunun ustune cikabilir. `evaluated` bayragi bunu bir kez
        // saymali — yoksa fren/gaz ile sinirsiz gecis puani basilirdi.
        val e = engine()
        e.startRun()
        e.advance(30)

        e.obstacles.clear()
        val target = GameEngine.Obstacle(
            lane = 0,
            x = e.laneCenter(0),
            y = e.playerY - 40f,
            colorIndex = 0,
            ownSpeed = 0f
        )
        e.obstacles.add(target)

        fun tickIsolated(frames: Int): List<GameEvent> {
            val out = ArrayList<GameEvent>()
            repeat(frames) {
                out.addAll(e.step(dt))
                // Motorun dogurdugu diger araclari at, hedefi koru.
                e.obstacles.removeAll { it !== target }
                if (!e.obstacles.contains(target)) e.obstacles.add(target)
            }
            return out
        }

        val first = tickIsolated(40)
        assertEquals(1, first.count { it is GameEvent.VehiclePassed })
        assertTrue(target.evaluated)

        // Araci elle geri yukari tasi (fren ile trafigin altina dusmus gibi)
        // ve bir kez daha gecir.
        target.y = e.playerY - 60f
        val second = tickIsolated(60)
        assertEquals("ayni arac ikinci kez sayildi", 0, second.count { it is GameEvent.VehiclePassed })
        assertEquals("ayni arac ikinci kez dodge verdi", 0, second.count { it is GameEvent.PerfectDodge })
    }

    @Test
    fun `varsayilan tabanda trafik hep gecilebilir kalir`() {
        // Trafigin en hizlisi bile MIN_SPEED'in altinda olmali: aksi halde tam
        // frende oyuncu trafigin altina duser ve hicbir araci gecemez.
        val fastest = GameConfig.BASE_SPEED * GameConfig.TRAFFIC_SPEED_RATIO_MAX
        assertTrue("trafik MIN_SPEED'i asiyor: $fastest", fastest < GameConfig.MIN_SPEED)

        LevelCatalog.levels.forEach { level ->
            val base = level.startSpeedKmh?.let { GameConfig.speedFromKmh(it) }
                ?: GameConfig.BASE_SPEED
            assertTrue(
                "bolum ${level.id}: trafik MIN_SPEED'i asiyor",
                base * GameConfig.TRAFFIC_SPEED_RATIO_MAX < GameConfig.MIN_SPEED
            )
        }
    }

    // -----------------------------------------------------------------
    // 13) Bolum trafik yogunlugu
    // -----------------------------------------------------------------

    @Test
    fun `trafficDensity dogma araligini bolumler`() {
        fun spawnCount(density: Float): Int {
            val level = LevelDef(
                id = 1,
                goal = LevelGoal.SurviveTime(60),
                stars = threeObjectives(),
                trafficDensity = density
            )
            val e = engine(RunMode.CAREER, level)
            e.startRun()
            var spawned = 0
            repeat(framesFor(30f)) {
                val before = e.obstacles.size
                e.step(dt)
                spawned += (e.obstacles.size - before).coerceAtLeast(0)
                e.obstacles.clear()
            }
            return spawned
        }

        val full = spawnCount(1f)
        val sparse = spawnCount(0.3f)
        val expectedFull = (30f / GameConfig.OBSTACLE_SPAWN_INTERVAL_SEC).toInt()
        val expectedSparse = (30f / (GameConfig.OBSTACLE_SPAWN_INTERVAL_SEC / 0.3f)).toInt()

        assertEquals("tam yogunluk", expectedFull.toFloat(), full.toFloat(), 2f)
        assertEquals("seyrek trafik", expectedSparse.toFloat(), sparse.toFloat(), 2f)
        assertTrue("yogunluk seyreltmedi: $full -> $sparse", sparse * 2 < full)
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

    // -----------------------------------------------------------------
    // 12) Arac carpanlari gercekten fizige giriyor mu (2026-08-15)
    // -----------------------------------------------------------------
    //
    // Her test AYNI yukseltme seviyesinde iki arac calistirir; tek degisken
    // govdedir. Karsilastirilan ciftler, olculen eksen DISINDA ayni carpana
    // sahip olacak sekilde secildi (ornegin fren testinde iki aracin da son
    // hiz carpani 1.00) — aksi halde test neyi olctugunu bilemezdi.

    /** Skordan gelen hiz tavani dolana kadar surer; ~64 s yeter. */
    private fun GameEngine.driveToTopSpeed(): Float {
        startRun()
        advance(framesFor(64f))
        return speed
    }

    @Test
    fun `son hiz carpani ayni yukseltmede farkli tavan verir`() {
        val upgrades = UpgradeLevels(speed = 4, acceleration = 4, brake = 4, boost = 4)
        val city = engine(upgrades = upgrades, shape = CarCatalog.defaultShape).driveToTopSpeed()
        val supercar = engine(
            upgrades = upgrades,
            shape = CarCatalog.shape(CarCatalog.SHAPE_SUPERCAR)
        ).driveToTopSpeed()

        assertTrue(
            "super araba daha yuksek tavana ulasmali: sehir=$city super=$supercar",
            supercar > city + 0.2f
        )
        // Fark yukseltmenin YERINE gecmiyor: seviye 8 Sehir, seviye 4 Super
        // Araba'yi hâlâ geciyor.
        val maxedCity = engine(
            upgrades = UpgradeLevels(speed = 8, acceleration = 8, brake = 8, boost = 8),
            shape = CarCatalog.defaultShape
        ).driveToTopSpeed()
        assertTrue(
            "yukseltme aracin onunde kalmali: maxSehir=$maxedCity sv4Super=$supercar",
            maxedCity > supercar
        )
    }

    @Test
    fun `ivme carpani hedefe yaklasma hizini degistirir`() {
        // Skor 0 iken hiz tavani baglayici degil; boost'a basinca iki aracin
        // HEDEFI ayni olur, yalnizca yaklasma orani ayrisir.
        fun speedAfterBoostFrames(shape: CarShapeDef): Float {
            val e = engine(shape = shape)
            e.startRun()
            e.setBoost(true)
            e.advance(5)
            return e.speed
        }

        val city = speedAfterBoostFrames(CarCatalog.defaultShape)
        val supercar = speedAfterBoostFrames(CarCatalog.shape(CarCatalog.SHAPE_SUPERCAR))
        val goat = speedAfterBoostFrames(CarCatalog.shape(CarCatalog.SHAPE_MOUNTAIN_GOAT))

        assertTrue("ivmesi yuksek arac daha cabuk toparlamali: $supercar > $city", supercar > city)
        assertTrue("ivmesi dusuk arac daha yavas toparlamali: $goat < $city", goat < city)
    }

    @Test
    fun `fren carpani ayni yukseltmede farkli yavaslama verir`() {
        // Araclarin son hiz tavani farkli oldugu icin MUTLAK hiz degil,
        // frenin ACTIGI FARK olculuyor: tavan hizi eksi frenli hiz.
        fun brakeDrop(shape: CarShapeDef): Float {
            val e = engine(shape = shape)
            val top = e.driveToTopSpeed()
            e.setBrake(true)
            e.advance(framesFor(3f))
            return top - e.speed
        }

        val city = brakeDrop(CarCatalog.defaultShape)
        val goat = brakeDrop(CarCatalog.shape(CarCatalog.SHAPE_MOUNTAIN_GOAT))
        val muscle67 = brakeDrop(CarCatalog.shape(CarCatalog.SHAPE_MUSCLE_67))

        assertTrue("freni iyi arac daha cok yavaslamali: kecisi=$goat sehir=$city", goat > city)
        assertTrue("freni zayif arac daha az yavaslamali: boga=$muscle67 sehir=$city", muscle67 < city)
    }

    @Test
    fun `boost carpani bar bosalma suresini degistirir`() {
        fun framesUntilEmpty(shape: CarShapeDef): Int {
            val e = engine(shape = shape)
            e.startRun()
            e.setBoost(true)
            var frames = 0
            while (e.boost > 0f && frames < 2000) {
                e.step(dt)
                e.obstacles.clear()
                e.coins.clear()
                frames++
            }
            return frames
        }

        val city = framesUntilEmpty(CarCatalog.defaultShape)
        val slx = framesUntilEmpty(CarCatalog.shape(CarCatalog.SHAPE_KUS_SLX))

        assertTrue("uzun boostlu arac barini daha gec bitirmeli: slx=$slx sehir=$city", slx > city)
        assertTrue("fark olculebilir olmali", slx - city >= 5)
    }

    @Test
    fun `carpisma kutusu AYNI SINIF icinde araca gore DEGISMEZ`() {
        // PROVENANCE #6 yeni hâli (2026-08-16): kutuyu degistiren tek sey
        // OLCU SINIFI; sinif icinde govde secimi carpismayi etkilemez.
        //
        // Sinif dagilimi DONDURULUYOR. Bir govde sessizce sinif degistirirse
        // bu bir DENGE degisikligidir — hedefler, yukseltme egrileri ve
        // dodge penceresi gozden gecirilmeli. Testin isi o degisikligi
        // gorunur kilmak, engellemek degil: beklenen dagilimi guncellemek
        // bilincli bir hareket olsun.
        val beklenenSinif = mapOf(
            CarCatalog.SHAPE_MOTOSIKLET to VehicleClass.MOTOSIKLET,
            CarCatalog.SHAPE_TIR to VehicleClass.AGIR
        )
        CarCatalog.shapes.forEach { shape ->
            assertEquals(
                "${shape.id}: olcu sinifi degisti — bu bir DENGE degisikligidir, " +
                    "hedefler ve yukseltme egrileri gozden gecirilmeli",
                beklenenSinif[shape.id] ?: VehicleClass.BINEK,
                shape.vehicleClass
            )
            val e = engine(shape = shape)
            e.startRun()
            e.advance(60)
            e.placeObstacleOnPlayer()
            e.step(dt)
            assertEquals("${shape.id}: carpisma davranisi degisti", RunPhase.CRASHED, e.phase)
        }
    }

    // -----------------------------------------------------------------
    // 14) Carpisma kutusu ARAC SINIFINDAN gelir (2026-08-16)
    //
    // Tasarim ve olculer: docs/VEHICLE_CLASSES.md. Bu bolumun tamaminin
    // dayandigi tek gercek Minkowski toplamidir: iki AABB'nin yatay carpma
    // esigi `(genislikA + genislikB) / 2`, dikey ortusme esigi
    // `(boyA + boyB) / 2`. Yani oyuncunun kutusunu daraltmak esigi yari
    // yariya daraltir — trafigin kutusu yerinde durur.
    //
    // Turetilen sayilar (CAR_ART_SCALE 0.80, HITBOX_SCALE 0.88):
    //   MOTOSIKLET 22x59   -> gorsel 17.60x47.20  hitbox 15.488x41.536
    //   BINEK      40x76   -> gorsel 32.00x60.80  hitbox 28.160x53.504
    //   AGIR       48x202  -> gorsel 38.40x161.60 hitbox 33.792x142.208
    // Binek trafige karsi yatay carpma esigi:
    //   motosiklet 21.824 · binek 28.160 · agir 30.976
    // -----------------------------------------------------------------

    /** Yalnizca OLCU SINIFI icin var olan bir test govdesi. */
    private fun classShape(vehicleClass: VehicleClass) = CarShapeDef(
        id = "test_${vehicleClass.name.lowercase()}",
        nameTr = "Test",
        nameEn = "Test",
        descriptionTr = "",
        descriptionEn = "",
        priceCoins = 0,
        requiredCarLevel = 1,
        // Dort surus carpani da 1.00 birakildi: karsilastirilan iki govde
        // arasindaki TEK degisken sinif olsun diye.
        parts = listOf(
            CarPart.Box(
                CarPaint.BODY,
                left = vehicleClass.artLeft,
                top = vehicleClass.artTop,
                width = vehicleClass.widthUnits,
                height = vehicleClass.lengthUnits
            )
        ),
        vehicleClass = vehicleClass
    )

    /**
     * Oyuncudan [dx] yanda, [dy] kadar yukarida/asagida duran tek bir engel
     * koyup BIR kare isletir ve carpisma olup olmadigini doner.
     *
     * `ownSpeed = speed` bilincli: yaklasma hizi ~0 olur, engel kurdugumuz
     * y'de kalir ve olculen sey yalnizca KUTU olur.
     */
    private fun crashesAt(
        playerShape: CarShapeDef,
        obstacleClass: VehicleClass,
        dx: Float,
        dy: Float = 0f
    ): Boolean {
        val e = engine(shape = playerShape)
        e.startRun()
        e.advance(30)
        e.obstacles.clear()
        e.obstacles.add(
            GameEngine.Obstacle(
                lane = e.playerLane,
                x = e.playerX + dx,
                y = e.playerY + dy,
                colorIndex = 0,
                ownSpeed = e.speed,
                vehicleClass = obstacleClass
            )
        )
        e.step(dt)
        return e.phase == RunPhase.CRASHED
    }

    /** [dx] yanindan gecen bir arac perfect dodge sayiliyor mu. */
    private fun dodgesAt(
        playerShape: CarShapeDef,
        obstacleClass: VehicleClass,
        dx: Float
    ): Boolean {
        val e = engine(shape = playerShape)
        e.startRun()
        e.advance(60)
        e.obstacles.clear()
        e.obstacles.add(
            GameEngine.Obstacle(
                lane = e.playerLane,
                x = e.playerX + dx,
                y = e.playerY + 10f,
                colorIndex = 0,
                ownSpeed = 0f,
                vehicleClass = obstacleClass
            )
        )
        val events = ArrayList<GameEvent>()
        repeat(30) { events.addAll(e.step(dt)) }
        assertEquals("test kurgusu: carpisma olmamaliydi", RunPhase.RUNNING, e.phase)
        assertTrue(
            "test kurgusu: arac gecis cizgisini asmadi",
            events.any { it is GameEvent.VehiclePassed }
        )
        return events.any { it is GameEvent.PerfectDodge }
    }

    @Test
    fun `BINEK kutusu GameConfig sabitleriyle BIT BAZINDA ayni`() {
        // CIPA TESTI. Motor kutuyu artik [VehicleClass]'tan turetiyor; binek
        // sinifi eski sabitlerden BIR BIT bile sapmamali, cunku butun bolum
        // hedefleri ve yukseltme egrileri o kutunun davranisina gore
        // ayarlandi. Delta 0f bilerek: yuvarlama farki bile kabul degil.
        val binek = VehicleClass.BINEK
        assertEquals(GameConfig.CAR_WIDTH_PX, binek.artWidthPx, 0f)
        assertEquals(GameConfig.CAR_HEIGHT_PX, binek.artHeightPx, 0f)
        assertEquals(GameConfig.CAR_HITBOX_WIDTH_PX, binek.hitboxWidthPx, 0f)
        assertEquals(GameConfig.CAR_HITBOX_HEIGHT_PX, binek.hitboxHeightPx, 0f)

        val artTopPx = binek.artTop * GameConfig.CAR_ART_SCALE
        val artBottomPx = binek.artBottom * GameConfig.CAR_ART_SCALE
        assertEquals(GameConfig.CAR_ART_TOP_OFFSET, artTopPx, 0f)
        assertEquals(GameConfig.CAR_ART_BOTTOM_OFFSET, artBottomPx, 0f)

        // GameEngine.HitboxSpec.hitTop ile AYNI ifade (ayni carpanlar, ayni
        // sirada) — motorun ozel alanina disaridan bakamadigimiz icin formul
        // burada birebir tekrarlaniyor.
        assertEquals(
            GameConfig.CAR_HITBOX_TOP_OFFSET,
            artTopPx + (binek.artHeightPx - binek.hitboxHeightPx) / 2f,
            0f
        )

        // Gecis cizgisi payi: eski kod `playerY + CAR_HEIGHT_PX` idi, yenisi
        // `playerY + (oyuncuArtAlt - engelArtUst)`. Binek ciftinde ayni sayi.
        assertEquals(GameConfig.CAR_HEIGHT_PX, artBottomPx - artTopPx, 0f)
    }

    @Test
    fun `binek carpisma esigi eski sabitin ta kendisi`() {
        // Davranissal cipa: esik (28.16 + 28.16) / 2 = CAR_HITBOX_WIDTH_PX.
        val binek = classShape(VehicleClass.BINEK)
        val threshold = GameConfig.CAR_HITBOX_WIDTH_PX
        assertTrue(
            "esigin hemen ICINDE carpmali",
            crashesAt(binek, VehicleClass.BINEK, dx = threshold - 0.05f)
        )
        assertFalse(
            "esigin hemen DISINDA carpmamali",
            crashesAt(binek, VehicleClass.BINEK, dx = threshold + 0.05f)
        )
        // Katalogdaki gercek govde de ayni esikte: sinif disinda hicbir sey
        // (surus carpanlari dahil) kutuya dokunmuyor.
        assertTrue(crashesAt(CarCatalog.defaultShape, VehicleClass.BINEK, threshold - 0.05f))
        assertFalse(crashesAt(CarCatalog.defaultShape, VehicleClass.BINEK, threshold + 0.05f))
    }

    @Test
    fun `dar arac dar kutuyla carpar`() {
        // 24 dp yanda duran bir trafik araci: binek esigi 28.16 (carpar),
        // motosiklet esigi 21.82 (carpmaz). Oyuncunun "yanimdaki bosluga
        // carptim" dedigi durum tam olarak burasi.
        val binek = classShape(VehicleClass.BINEK)
        val motosiklet = classShape(VehicleClass.MOTOSIKLET)

        assertTrue(
            "binek bu mesafede carpmali",
            crashesAt(binek, VehicleClass.BINEK, dx = 24f)
        )
        assertFalse(
            "motosiklet dar kutusuyla bu mesafede carpMAMALI",
            crashesAt(motosiklet, VehicleClass.BINEK, dx = 24f)
        )

        // Dar kutu "carpismaz" demek degil: esigin icinde ikisi de carpar.
        assertTrue(crashesAt(motosiklet, VehicleClass.BINEK, dx = 20f))
        assertTrue(crashesAt(binek, VehicleClass.BINEK, dx = 20f))

        // Ve avantaj Minkowski toplami yuzunden sanildigi kadar buyuk degil:
        // oyuncunun kutusu %45 daralirken esik yalnizca %22.5 daraliyor.
        val binekThreshold =
            (VehicleClass.BINEK.hitboxWidthPx + VehicleClass.BINEK.hitboxWidthPx) / 2f
        val motoThreshold =
            (VehicleClass.MOTOSIKLET.hitboxWidthPx + VehicleClass.BINEK.hitboxWidthPx) / 2f
        assertEquals(28.16f, binekThreshold, 0.01f)
        assertEquals(21.824f, motoThreshold, 0.01f)
    }

    @Test
    fun `uzun arac uzun kutuyla carpar`() {
        // Engel oyuncunun 100 dp ONUNDE dogsun. Binek gövdesi (hitbox boyu
        // 53.5) o mesafede oyuncuya degmez; tirin kutusu 142.2 uzunlugunda,
        // yani BURNU 100 dp ileride olsa bile KUYRUGU hâlâ oyuncunun
        // yanindadir. Tek sabit kutuyla tirin icinden gecilebilirdi.
        val binek = classShape(VehicleClass.BINEK)

        assertFalse(
            "binek engel bu mesafede degmemeli",
            crashesAt(binek, VehicleClass.BINEK, dx = 0f, dy = -100f)
        )
        assertTrue(
            "tirin govdesi bu mesafede hâlâ oyuncunun yaninda",
            crashesAt(binek, VehicleClass.AGIR, dx = 0f, dy = -100f)
        )

        // Uzunluk sonsuz degil: kendi kutusunun disinda tir da degmez.
        assertFalse(
            "tir kendi kutusunun disinda carpmamali",
            crashesAt(binek, VehicleClass.AGIR, dx = 0f, dy = -160f)
        )
    }

    @Test
    fun `genis arac genis kutuyla carpar`() {
        // 29.5 dp yan mesafe: binek esigi 28.16 (temiz gecis), tir esigi
        // 30.98 (carpma). Tirin genisligi de kutuya giriyor.
        val binek = classShape(VehicleClass.BINEK)
        assertFalse(crashesAt(binek, VehicleClass.BINEK, dx = 29.5f))
        assertTrue(crashesAt(binek, VehicleClass.AGIR, dx = 29.5f))
    }

    @Test
    fun `trafik kendi govdesinin sinifiyla dogar`() {
        val e = engine()
        e.startRun()
        // 3 saniye = dogma araligi (0.78 s) x ~4 arac.
        e.advance(framesFor(3f), clearTraffic = false)
        assertTrue("test kurgusu: hic arac dogmadi", e.obstacles.isNotEmpty())
        e.obstacles.forEach {
            assertEquals(CarCatalog.trafficShape.vehicleClass, it.vehicleClass)
        }
    }

    // -----------------------------------------------------------------
    // 15) Perfect dodge penceresi sinifla birlikte kaymaz
    //
    // docs/VEHICLE_CLASSES.md §2: esik sabit kalir da carpma esigi
    // daralirsa, dar arac hem daha az carpar HEM daha cok combo yapar —
    // bilesik ve istenmeyen bir avantaj. Pencere, iki aracin GORUNEN
    // kenarlari arasindaki acikliktir; taban terim cift bazindadir.
    // -----------------------------------------------------------------

    @Test
    fun `perfect dodge esigi binek ciftinde GameConfig ile ayni kalir`() {
        val binek = classShape(VehicleClass.BINEK)
        val e = engine()
        val threshold = GameConfig.perfectDodgeMaxDx(e.laneWidth)
        assertEquals("360 dp'de esik 49.6 olmali", 49.6f, threshold, 0.01f)

        assertTrue(
            "esigin hemen ICINDE dodge sayilmali",
            dodgesAt(binek, VehicleClass.BINEK, dx = threshold - 0.5f)
        )
        assertFalse(
            "esigin hemen DISINDA dodge sayilmamali",
            dodgesAt(binek, VehicleClass.BINEK, dx = threshold + 0.5f)
        )
    }

    @Test
    fun `perfect dodge penceresi dar aracta genislemez`() {
        // Motosiklet cifti icin taban terim (17.6 + 32) / 2 = 24.8, yani
        // esik 24.8 + 17.6 = 42.4 dp. Esik BINEK'inki gibi 49.6'da kalsaydi
        // motosikletin penceresi (21.83 , 49.6] = 27.78 dp olurdu; binegin
        // penceresi 21.44 dp, yani %30 daha genis. Duzeltmeyle 20.58 dp.
        val motosiklet = classShape(VehicleClass.MOTOSIKLET)
        val e = engine()
        val binekThreshold = GameConfig.perfectDodgeMaxDx(e.laneWidth)

        assertFalse(
            "binek esigi motosiklete oldugu gibi uygulanmis (pencere genisledi)",
            dodgesAt(motosiklet, VehicleClass.BINEK, dx = binekThreshold - 0.5f)
        )
        assertTrue(
            "motosiklet kendi penceresi icinde dodge yapabilmeli",
            dodgesAt(motosiklet, VehicleClass.BINEK, dx = 41f)
        )

        // Pencere GENISLIKLERI (esik eksi carpma esigi) birbirine yakin
        // kalmali: iki arac da ayni "beceri" karsiliginda ayni odulu alsin.
        val binekWindow = binekThreshold -
            (VehicleClass.BINEK.hitboxWidthPx + VehicleClass.BINEK.hitboxWidthPx) / 2f
        val pairBase =
            (VehicleClass.MOTOSIKLET.artWidthPx + VehicleClass.BINEK.artWidthPx) / 2f
        val motoThreshold = binekThreshold + (pairBase - GameConfig.CAR_WIDTH_PX)
        val motoWindow = motoThreshold -
            (VehicleClass.MOTOSIKLET.hitboxWidthPx + VehicleClass.BINEK.hitboxWidthPx) / 2f
        assertEquals(21.44f, binekWindow, 0.01f)
        assertEquals(20.576f, motoWindow, 0.01f)
        assertTrue(
            "pencere genisligi sinifa gore %10'dan fazla kaymamali: " +
                "binek=$binekWindow moto=$motoWindow",
            abs(motoWindow - binekWindow) / binekWindow < 0.10f
        )
    }

    // -----------------------------------------------------------------
    // 16) Serit uyumu (docs/VEHICLE_CLASSES.md §3)
    // -----------------------------------------------------------------

    @Test
    fun `her sinif cifti en dar seritte komsu serit merkezine tasmaz`() {
        // Desteklenen en dar ekran 320 dp: yol min(290, 320*0.56) = 179.2,
        // serit 59.73 dp. Komsu seritteki bir arac oyuncuya DEGMEMELI,
        // yoksa gozle bariz bosluk varken kaza olur (2026-08-13 hatasi).
        val e = engine()
        e.setViewport(320f, 640f)
        assertEquals("en dar serit", 59.733f, e.laneWidth, 0.01f)

        VehicleClass.entries.forEach { a ->
            assertTrue(
                "$a cizimi seride sigmiyor: ${a.artWidthPx} > ${e.laneWidth}",
                a.artWidthPx < e.laneWidth
            )
            VehicleClass.entries.forEach { b ->
                val threshold = (a.hitboxWidthPx + b.hitboxWidthPx) / 2f
                assertTrue(
                    "$a + $b carpma esigi serit araligini asiyor: " +
                        "$threshold >= ${e.laneWidth}",
                    threshold < e.laneWidth
                )
            }
        }
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

    // -----------------------------------------------------------------
    // Hiz rampasi (2026-08-16)
    // -----------------------------------------------------------------

    /**
     * [LevelDef.speedRampScale] carpani `min(hizTavani, skor/600)` ifadesinin
     * SONUCUNA uygulanir — yani tavani SABITLEMEZ, ORANTILI KUCULTUR.
     *
     * Bu test o secimi kilitliyor ve sebebi su: reddedilen alternatif
     * (yalnizca rampayi yavaslatip tavani birakmak) kagit uzerinde ayni
     * seye benziyor ama tam ters sonuc veriyor — tavan degismedigi icin
     * kosu sonunda tepki butcesi hic iyilesmiyor ve tam yukseltmeli
     * oyuncuda HICBIR SEY degismiyor (docs/REVIEW_GAMEPLAY.md).
     *
     * Test kirilirsa yapilacak sey esigi gevsetmek DEGIL, hangi terimin
     * carpildigina bakmaktir.
     */
    @Test
    fun `hiz rampasi tavani orantili kuculturur, sabitlemez`() {
        val sigma = 0.65f
        // Yildiz veren bolumde TAM OLARAK uc hedef sart (LevelDef init'i
        // zorluyor); bu test hedeflerle ilgilenmiyor, ucu de kolay.
        fun bolum(ramp: Float) = LevelDef(
            id = 99,
            goal = LevelGoal.SurviveTime(120),
            stars = listOf(
                Objective.CompleteRun,
                Objective.PassVehicles(1),
                Objective.CoinsAtLeast(1)
            ),
            speedRampScale = ramp
        )

        // Ayni tohum, ayni sure: tek degisken rampa.
        fun hiz(ramp: Float, speedLevel: Int): Float {
            val e = engine(
                level = bolum(ramp),
                upgrades = UpgradeLevels(speed = speedLevel)
            )
            e.startRun()
            e.advance(90)
            return e.speed
        }

        val tamRampa = hiz(1f, 1)
        val yavasRampa = hiz(sigma, 1)
        assertTrue(
            "rampa carpani hizi dusurmeli ($tamRampa -> $yavasRampa)",
            yavasRampa < tamRampa
        )

        // ASIL IDDIA: yukseltmenin degeri ORANTILI korunuyor. Tavan
        // sabitlenseydi yukseltmeli ve yukseltmesiz oyuncu ayni hiza
        // yapisirdi ve asagidaki fark SIFIRA inerdi.
        val yukseltmesizFark = hiz(1f, 1) - hiz(sigma, 1)
        val yukseltmeliFark = hiz(1f, 8) - hiz(sigma, 8)
        assertTrue(
            "yukseltmeli oyuncuda rampanin etkisi kaybolmus — carpan yanlis " +
                "terime uygulanmis olabilir (yukseltmesiz $yukseltmesizFark, " +
                "yukseltmeli $yukseltmeliFark)",
            yukseltmeliFark > yukseltmesizFark * 0.9f
        )
    }

    /**
     * Rampa YALNIZCA ogrenme bolumlerinde. 8. bolumden sonrasi tam hizda
     * kalir; aksi halde butun kariyer yavaslatilmis olurdu ve bu bir denge
     * degisikligi degil, oyunun karakterinin degismesi olurdu.
     */
    @Test
    fun `hiz rampasi yalnizca ilk yedi bolumde`() {
        LevelCatalog.levels.forEach { level ->
            if (level.id <= 7) {
                assertTrue(
                    "bolum ${level.id}: ogrenme bolumu tam rampada " +
                        "(${level.speedRampScale}) — kolaylastirma uygulanmamis",
                    level.speedRampScale < 1f
                )
            } else {
                assertEquals(
                    "bolum ${level.id}: ogrenme bolumu degil, rampa 1.0 olmali",
                    1f,
                    level.speedRampScale,
                    0f
                )
            }
        }
        // Rampa MONOTON artmali: bolum ilerledikce oyun hizlanmali.
        val ogrenme = LevelCatalog.levels.filter { it.id <= 7 }
        ogrenme.zipWithNext().forEach { (a, b) ->
            assertTrue(
                "rampa geriye gitti: bolum ${a.id} (${a.speedRampScale}) -> " +
                    "${b.id} (${b.speedRampScale})",
                b.speedRampScale > a.speedRampScale
            )
        }
    }

}
