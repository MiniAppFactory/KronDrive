package com.miniappfactory.krondrive.game

import com.miniappfactory.krondrive.data.BoosterType
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

/**
 * Kron Drive simulasyonu. **Saf Kotlin** — Android'e hic bagimliligi yok, bu
 * sayede JVM birim testleriyle dogrulanabiliyor (bkz. `src/test/.../game`).
 * Cizim [com.miniappfactory.krondrive.ui.game] altinda, bu sinifin durumunu
 * okuyup Compose Canvas'a aktarir.
 *
 * Fizik, HTML prototipinden birebir tasindi; tek yapisal fark, ACCELERATION
 * yukseltmesinin anlamli olabilmesi icin eklenen birinci dereceden hiz
 * gecikmesidir (bkz. [GameConfig.ACCEL_RATE_BASE]).
 */
class GameEngine(
    val mode: RunMode,
    val level: LevelDef? = null,
    private val upgrades: UpgradeLevels = UpgradeLevels(),
    private val boosters: Set<BoosterType> = emptySet(),
    /** Sonsuz moddaki mevcut rekor (saniye) — "rekoruna 3 saniye kaldi" mesaji icin. */
    private val endlessRecordSeconds: Int = 0,
    /**
     * Bu bolumden DAHA ONCE kazanilmis yildiz sayisi. Coin odulu yalnizca
     * bunun USTUNE cikan yildizlar icin verilir; aksi halde ayni bolumu
     * tekrar oynamak sinirsiz coin basardi (bkz. finish()).
     */
    private val previousStars: Int = 0,
    /**
     * Oyuncunun garajda sectigi govde sekli ve boyasi.
     *
     * 2026-08-15'e kadar simulasyona HIC dokunmuyordu (araclar tamamen
     * kozmetikti). Artik govdenin dort surus carpani fizige giriyor — bkz.
     * [CarShapeDef] ve `docs/BALANCE.md`. Boya hâlâ yalnizca cizimdir.
     *
     * CARPISMA KUTUSU GOVDENIN OLCU SINIFINDAN GELIR (2026-08-16, bkz.
     * [CarShapeDef.vehicleClass] ve `docs/VEHICLE_CLASSES.md`). Dort surus
     * carpani kutuya hâlâ DOKUNMAZ; degistiren tek sey [VehicleClass]'tir ve
     * katalogdaki butun govdeler bugun [VehicleClass.BINEK] sinifinda, yani
     * PROVENANCE #6'nin "arac secimi carpismayi degistirmez" kurali pratikte
     * korunuyor. Kural yeni hâliyle: **ayni sinif icinde kutu degismez.**
     */
    val carStyle: CarStyle = CarCatalog.defaultStyle,
    private val random: Random = Random.Default
) {

    /**
     * Fizige giren govde. Kosu boyunca degismez — oyuncu garaja donmeden
     * arac degistiremez, bu yuzden her karede [carStyle] uzerinden cozmek
     * yerine bir kez alaniyor.
     */
    private val car: CarShapeDef = carStyle.shape

    // -----------------------------------------------------------------
    // Sahne olculeri
    // -----------------------------------------------------------------

    var viewWidth: Float = 0f
        private set
    var viewHeight: Float = 0f
        private set

    var roadWidth: Float = 0f
        private set
    var roadX: Float = 0f
        private set
    var laneWidth: Float = 0f
        private set

    fun setViewport(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        viewWidth = width
        viewHeight = height
        roadWidth = min(GameConfig.ROAD_MAX_WIDTH_PX, width * GameConfig.ROAD_WIDTH_RATIO)
        roadX = (width - roadWidth) / 2f
        laneWidth = roadWidth / GameConfig.LANE_COUNT
        playerY = height - GameConfig.PLAYER_BOTTOM_OFFSET_PX
        targetX = laneCenter(playerLane)
        if (playerX == 0f) playerX = targetX
        // Ekran olculeri degistiginde serit merkezleri kayar; sahnedeki
        // nesneleri de yeni merkezlere tasi (aksi halde engeller yolun
        // disinda kalirdi).
        obstacles.forEach { it.x = laneCenter(it.lane) }
        coins.forEach { it.x = laneCenter(it.lane) }
    }

    fun laneCenter(lane: Int): Float = roadX + laneWidth * lane + laneWidth / 2f

    // -----------------------------------------------------------------
    // Kosu durumu
    // -----------------------------------------------------------------

    var phase: RunPhase = RunPhase.COUNTDOWN
        private set

    /** Geri sayimda kalan saniye (3, 2, 1 ...). */
    var countdownRemaining: Float = GameConfig.COUNTDOWN_SECONDS.toFloat()
        private set

    // GECICI: olcum icin tema sabitlendi (PERF_MEASUREMENT_20260817). Geri alinacak.
    val theme: RoadTheme = RoadTheme.CROWD
        .also { random.nextInt(RoadTheme.entries.size) } // RNG akisi bozulmasin

    /** Bu kosunun taban hizi — bolum kendi baslangic hizini verebilir. */
    private val baseSpeed: Float =
        level?.startSpeedKmh?.let { GameConfig.speedFromKmh(it) } ?: GameConfig.BASE_SPEED

    var score: Float = 0f
        private set
    var speed: Float = baseSpeed
        private set
    var boost: Float = GameConfig.BOOST_START
        private set
    var roadOffset: Float = 0f
        private set

    var timeElapsed: Float = 0f
        private set

    /** Sure hedefli bolumlerde kalan sure; sonsuz modda null. */
    var timeRemaining: Float? = null
        private set

    private var distancePx: Float = 0f
    private var boostDistancePx: Float = 0f

    var combo: Int = 0
        private set
    private var comboTimer: Float = 0f

    private var bestCombo: Int = 0
    private var bigCombos: Int = 0
    private var perfectDodges: Int = 0
    private var vehiclesPassed: Int = 0
    private var coinsCollected: Int = 0
    private var brakeTaps: Int = 0
    private var crashed: Boolean = false
    private var revivesUsed: Int = 0

    private var invulnerableFor: Float = 0f
    private var freeBoostRemaining: Float =
        if (BoosterType.TURBO_START in boosters) GameConfig.TURBO_START_FREE_BOOST_SEC else 0f
    private var secondChanceAvailable: Boolean = BoosterType.SECOND_CHANCE in boosters

    /** Bar bosaldi ve parmak hala basili — boost yeniden tutusamaz. */
    private var boostLockedUntilRelease: Boolean = false

    private val scoreMultiplier: Float =
        if (BoosterType.SCORE_BOOSTER in boosters) GameConfig.SCORE_BOOSTER_MULTIPLIER else 1f

    /** Kosu bittiginde doldurulur; sonuc ekrani bunu okur. */
    var lastResult: RunResult? = null
        private set

    // -----------------------------------------------------------------
    // Oyuncu ve giris
    // -----------------------------------------------------------------

    var playerLane: Int = 1
        private set
    var playerX: Float = 0f
        private set
    var playerY: Float = 0f
        private set
    private var targetX: Float = 0f

    var boostDown: Boolean = false
        private set
    var brakeDown: Boolean = false
        private set

    /** HUD'daki "boost yaniyor" gostergesi ve egzoz alevi icin. */
    var boosting: Boolean = false
        private set

    // -----------------------------------------------------------------
    // Sahne nesneleri
    // -----------------------------------------------------------------

    class Obstacle(
        var lane: Int,
        var x: Float,
        var y: Float,
        val colorIndex: Int,
        /**
         * Aracin KENDI ileri hizi, oyuncunun hiziyla ayni birimde.
         * Ekranda asagi akma hizi `(oyuncuHizi - ownSpeed) * K`; zemin
         * `oyuncuHizi * K` ile kaydigi icin arac asfalta gore `ownSpeed`
         * kadar ILERI gider. 0 = park etmis arac (eski davranis).
         * Bkz. [GameConfig.TRAFFIC_SPEED_RATIO_MIN].
         */
        val ownSpeed: Float,
        /**
         * Bu aracin OLCU SINIFI — carpisma kutusu ve gecis cizgisi bundan
         * turer (bkz. [HitboxSpec]).
         *
         * Varsayilan deger [CarCatalog.trafficShape]'ten okunur, sabit
         * yazilmaz: trafik govdesinin sinifi degistigi gun elle kurulan
         * engeller (testler, ileride ozel dogum kurallari) sessizce eski
         * sinifta kalmasin diye.
         */
        val vehicleClass: VehicleClass = CarCatalog.trafficShape.vehicleClass
    ) {
        /** Oyuncuyla dikey olarak ortusurken gorulen en kucuk yatay mesafe. */
        var minDx: Float = Float.MAX_VALUE
        /** Oyuncuyu geride biraktiginda bir kez degerlendirilir. */
        var evaluated: Boolean = false
    }

    /**
     * Bir OLCU SINIFININ carpisma geometrisi, piksel cinsinden ve ARACIN
     * y'sine gore. Sinif basina bir kez kurulur ([hitboxOf]) — 60 Hz'de her
     * engel icin yeniden turetmemek icin.
     *
     * Buradaki her deger [VehicleClass] ve [GameConfig] uzerinden TURETILIR;
     * elle yazilmis tek bir sayi yok. [VehicleClass.BINEK] icin sonuclar
     * [GameConfig.CAR_HITBOX_WIDTH_PX], [GameConfig.CAR_HITBOX_HEIGHT_PX] ve
     * [GameConfig.CAR_HITBOX_TOP_OFFSET] ile **bit bazinda** ayni cikar:
     * ayni carpanlar ayni sirada uygulaniyor. Bu bir tesaduf degil sart —
     * binek kutusu butun bolum hedeflerinin ve yukseltme egrilerinin
     * cipasi. `GameEngineTest` bunu ayri bir testle donduruyor.
     */
    private class HitboxSpec(vehicleClass: VehicleClass) {
        /** Carpisma kutusu genisligi. */
        val width: Float = vehicleClass.hitboxWidthPx

        /** Carpisma kutusu boyu. */
        val height: Float = vehicleClass.hitboxHeightPx

        /** Cizimin ust kenari (aracin y'sine gore). */
        val artTop: Float = vehicleClass.artTop * GameConfig.CAR_ART_SCALE

        /** Cizimin alt kenari (aracin y'sine gore). */
        val artBottom: Float = vehicleClass.artBottom * GameConfig.CAR_ART_SCALE

        /** Gorunur genislik — perfect dodge penceresi bunu kullanir. */
        val artWidth: Float = vehicleClass.artWidthPx

        /** Carpisma kutusunun ust kenari: gorselin icinde ORTALI. */
        val hitTop: Float = artTop + (vehicleClass.artHeightPx - height) / 2f
    }

    class Coin(var lane: Int, var x: Float, var y: Float, var pulse: Float)

    class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        val size: Float,
        val color: Int
    )

    val obstacles = ArrayList<Obstacle>()
    val coins = ArrayList<Coin>()
    val particles = ArrayList<Particle>()

    private var obstacleSpawnAcc: Float = 0f
    private var coinSpawnAcc: Float = 0f

    /** Sifirdan buyukken yeni arac dogmaz (revive sonrasi guvenli pencere). */
    private var spawnPausedFor: Float = 0f

    private val events = ArrayList<GameEvent>()

    init {
        timeRemaining = level?.goal?.timeLimitSeconds?.toFloat()
    }

    // -----------------------------------------------------------------
    // Giris (kontroller)
    // -----------------------------------------------------------------

    fun steerLeft() {
        if (phase != RunPhase.RUNNING) return
        playerLane = (playerLane - 1).coerceAtLeast(0)
        targetX = laneCenter(playerLane)
    }

    fun steerRight() {
        if (phase != RunPhase.RUNNING) return
        playerLane = (playerLane + 1).coerceAtMost(GameConfig.LANE_COUNT - 1)
        targetX = laneCenter(playerLane)
    }

    fun setBoost(down: Boolean) {
        boostDown = down
    }

    // -----------------------------------------------------------------
    // Hiz kilidi — YALNIZCA sonsuz mod
    // -----------------------------------------------------------------

    /**
     * Sonsuz modda hiz kilidi acik mi. Sahibi istegi (2026-08-15): "uzun sure
     * gitmek isteyen varsa sabitlesin, yavas yavas gitsin".
     *
     * Kilit ACIKKEN skordan gelen hizlanma ve sonsuz modun 30 saniyelik hiz
     * rampasi DEVRE DISI kalir; hiz kilitlendigi degerde durur. Boost ve fren
     * calismaya devam eder — kilit "hizini sen yonet" demek, "donduruldu"
     * demek degil.
     *
     * TRAFIK YOGUNLUGU RAMPASI DURMAZ: mod yine zorlasir, sadece oyuncu
     * kendi temposunu secer. Aksi halde kilit, sonsuz modu sonsuz bir
     * duz yola cevirirdi.
     */
    var speedLocked: Boolean = false
        private set

    /** Kilitlendigi andaki hedef hiz; kilit acikken taban budur. */
    private var lockedSpeed: Float = 0f

    /** Hiz kilidi bu modda kullanilabilir mi (yalnizca sonsuz mod). */
    fun canLockSpeed(): Boolean = mode == RunMode.ENDLESS

    fun toggleSpeedLock() {
        if (!canLockSpeed()) return
        speedLocked = !speedLocked
        if (speedLocked) lockedSpeed = speed
    }

    fun setBrake(down: Boolean) {
        // "Frene en fazla N kez bas" hedefi icin sadece BASMA kenari sayilir.
        if (down && !brakeDown && phase == RunPhase.RUNNING) brakeTaps++
        brakeDown = down
    }

    fun pause() {
        if (phase == RunPhase.RUNNING || phase == RunPhase.COUNTDOWN) {
            resumePhase = phase
            phase = RunPhase.PAUSED
            boostDown = false
            brakeDown = false
            boosting = false
        }
    }

    private var resumePhase: RunPhase = RunPhase.COUNTDOWN

    fun resume() {
        if (phase == RunPhase.PAUSED) phase = resumePhase
    }

    /**
     * Reklam izlendikten sonra carpisma noktasindan devam. Skor ve istatistikler
     * korunur, oyuncunun onundeki trafik temizlenir ve kisa bir dokunulmazlik
     * verilir — aksi halde oyuncu ayni araca aninda tekrar carpardi.
     */
    fun revive() {
        if (phase != RunPhase.CRASHED) return
        revivesUsed++
        crashed = false
        // TUM araclar temizlenir — eskiden yalnizca EKRANDAKILER siliniyordu,
        // ekranin ustunde bekleyenler (y < -CAR_HEIGHT) duruyor ve dokunulmazlik
        // biter bitmez oyuncunun uzerine iniyordu. Oyuncu bunu "reklami izledim,
        // baslar baslamaz tekrar carptim" diye bildirdi (2026-08-14).
        obstacles.clear()
        invulnerableFor = GameConfig.INVULNERABLE_SEC_AFTER_SAVE
        // Bos yolun hemen yeni araclarla dolmamasi icin dogma da duraklatilir.
        spawnPausedFor = GameConfig.REVIVE_SPAWN_PAUSE_SEC
        obstacleSpawnAcc = 0f
        boost = GameConfig.BOOST_MAX
        combo = 0
        comboTimer = 0f
        phase = RunPhase.RUNNING
    }

    // -----------------------------------------------------------------
    // GECICI OLCUM KANCASI — PERF_MEASUREMENT_20260817, commit EDILMEZ
    // -----------------------------------------------------------------
    private var pFrames = 0
    private var pWall = 0.0          // gercek gecen sure (kirpilmamis dt toplami)
    private var pSim = 0.0           // simulasyona verilen sure (kirpilmis dt toplami)
    private var pClip32 = 0          // dt > 0.032 olan kare sayisi
    private var pLost32 = 0.0        // 0.032 sinirinda kaybolacak toplam sure
    private var pClipCap = 0         // dt > MAX_FRAME_DT olan kare sayisi
    private var pLostCap = 0.0       // mevcut sinirda GERCEKTEN kaybolan sure
    private val pBuckets = IntArray(7)

    private fun perfProbe(raw: Float) {
        if (raw <= 0f) return
        pFrames++
        pWall += raw
        pSim += raw.coerceIn(0f, GameConfig.MAX_FRAME_DT)
        if (raw > 0.032f) { pClip32++; pLost32 += (raw - 0.032f) }
        if (raw > GameConfig.MAX_FRAME_DT) { pClipCap++; pLostCap += (raw - GameConfig.MAX_FRAME_DT) }
        val ms = raw * 1000f
        val b = when {
            ms < 20f -> 0      // 1 vsync (60 Hz)
            ms < 28f -> 1
            ms < 36f -> 2      // 2 vsync
            ms < 45f -> 3
            ms < 56f -> 4      // 3 vsync
            ms < 75f -> 5      // 4 vsync
            else -> 6
        }
        pBuckets[b]++
        if (pFrames % 120 == 0) {
            println(
                "KDPERF cap=${GameConfig.MAX_FRAME_DT} theme=$theme frames=$pFrames " +
                    "wall=${"%.2f".format(pWall)}s sim=${"%.2f".format(pSim)}s " +
                    "drift=${"%.0f".format((pWall - pSim) * 1000)}ms " +
                    "avgdt=${"%.1f".format(pWall / pFrames * 1000)}ms " +
                    "fps=${"%.1f".format(pFrames / pWall)} " +
                    "clip32=$pClip32(${"%.1f".format(100.0 * pClip32 / pFrames)}%) " +
                    "lost32=${"%.0f".format(pLost32 * 1000)}ms(${"%.1f".format(pLost32 / pWall * 1000)}ms/s) " +
                    "clipCap=$pClipCap(${"%.1f".format(100.0 * pClipCap / pFrames)}%) " +
                    "lostCap=${"%.0f".format(pLostCap * 1000)}ms(${"%.1f".format(pLostCap / pWall * 1000)}ms/s) " +
                    "buckets=${pBuckets.joinToString(",")}"
            )
        }
    }

    // -----------------------------------------------------------------
    // Ana dongu
    // -----------------------------------------------------------------

    /**
     * Bir kareyi isler ve o karede olusan olaylari dondurur.
     *
     * Olay listesi kare BASINDA degil SONUNDA bosaltilir: `finish()` gibi disaridan
     * cagrilabilen fonksiyonlarin urettigi olaylar da bir sonraki cagrida cagirana
     * ulassin diye (bastan temizleseydik sessizce kaybolurlardi).
     */
    fun step(deltaSeconds: Float): List<GameEvent> {
        perfProbe(deltaSeconds)
        val dt = deltaSeconds.coerceIn(0f, GameConfig.MAX_FRAME_DT)
        when (phase) {
            RunPhase.COUNTDOWN -> {
                countdownRemaining -= dt
                if (countdownRemaining <= 0f) {
                    countdownRemaining = 0f
                    phase = RunPhase.RUNNING
                }
            }

            RunPhase.RUNNING -> updateRunning(dt)
            RunPhase.PAUSED, RunPhase.CRASHED, RunPhase.FINISHED -> Unit
        }
        updateParticles(dt)
        // Cogu karede hic olay yok — o durumda tahsisat da yapmiyoruz.
        if (events.isEmpty()) return emptyList()
        val produced = ArrayList<GameEvent>(events)
        events.clear()
        return produced
    }

    private fun updateRunning(dt: Float) {
        timeElapsed += dt
        if (invulnerableFor > 0f) invulnerableFor -= dt

        updateSpeed(dt)
        updateBoostEnergy(dt)

        // Serit degisiminin yumusak takibi (prototiple ayni katsayi).
        playerX += (targetX - playerX) * min(1f, GameConfig.LANE_LERP_RATE * dt)

        val travel = speed * GameConfig.WORLD_PX_PER_SPEED_UNIT * dt
        roadOffset += travel
        distancePx += travel
        if (boosting) boostDistancePx += travel

        score += speed * GameConfig.SCORE_PER_SPEED_PER_SEC * dt * scoreMultiplier

        updateCombo(dt)
        spawn(dt)
        updateObstacles(dt)
        updateCoins(dt)

        timeRemaining?.let { remaining ->
            val left = remaining - dt
            timeRemaining = left
            if (left <= 0f) onTimeLimitReached()
        }
        checkGoalReached()
    }

    /** Bolumun hiz rampasi carpani; bolum yoksa (sonsuz mod) tam rampa. */
    private val speedRamp: Float = level?.speedRampScale ?: 1f

    private fun updateSpeed(dt: Float) {
        val scoreCap = UpgradeCatalog.scoreSpeedCap(upgrades.speed, car)
        // Kilit acikken skordan gelen hizlanma yok sayilir (bkz. speedLocked).
        var target = if (speedLocked) {
            lockedSpeed
        } else {
            // Carpan min()'in SONUCUNA uygulanir — tavani sabitlemez,
            // ORANTILI kucultur. Gerekce ve reddedilen alternatif:
            // [LevelDef.speedRampScale]. Bunu "yalnizca rampaya uygula"
            // diye degistirme; olculup reddedildi.
            baseSpeed + speedRamp * min(scoreCap, score / GameConfig.SCORE_SPEED_DIVISOR)
        }

        // Parmak kalkinca kilit acilir.
        if (!boostDown) boostLockedUntilRelease = false
        // Histerezis: devam etmek icin enerji > 0 yeter, ama bar bosaldiktan
        // sonra YENIDEN tutusmak icin hem parmagin kalkmasi hem de
        // BOOST_REENGAGE_MIN kadar enerji gerekir (bkz. GameConfig).
        val energyThreshold = if (boosting) 0f else GameConfig.BOOST_REENGAGE_MIN
        val wantsBoost = boostDown &&
            !boostLockedUntilRelease &&
            (boost > energyThreshold || freeBoostRemaining > 0f)
        if (wantsBoost) target += UpgradeCatalog.boostSpeedBonus(upgrades.boost)
        if (brakeDown) target -= UpgradeCatalog.brakePenalty(upgrades.brake, car)
        // Sonsuz modun 30 saniyelik hiz rampasi da kilitliyken uygulanmaz;
        // trafik yogunlugu rampasi ise devam eder (bkz. speedLocked).
        if (!speedLocked) target *= endlessSpeedMultiplier()
        target = target.coerceAtLeast(GameConfig.MIN_SPEED)

        val rate = if (target > speed) {
            UpgradeCatalog.accelRate(upgrades.acceleration, car)
        } else {
            // Aracin fren carpani BURAYA UYGULANMAZ, bilerek. [decelRate]
            // yalnizca frenin degil HER asagi yoneli yakinsamanin orani:
            // boost birakildiktan sonraki sonumleme de bundan gecer. Carpanla
            // olceklenseydi "freni iyi" bir arac boost'un artigini da daha
            // cabuk kaybederdi — yani bir GUC, gizli bir CEZA getirirdi.
            // Ayrica garajdaki FREN cubugu [brakePenalty] uzerinden okunuyor;
            // gosterilmeyen ikinci bir etki eklemek "gosterilmeyen ozellik yok
            // sayilir" kuralini bozardi (docs/BALANCE.md, sinir 3). Ayni hata
            // yukseltme tarafinda bir kez yapilmisti (bkz. UpgradeCatalog).
            UpgradeCatalog.decelRate(upgrades.brake)
        }
        speed += (target - speed) * min(1f, rate * dt)

        if (wantsBoost && !boosting) events.add(GameEvent.BoostStarted)
        boosting = wantsBoost
    }

    private fun updateBoostEnergy(dt: Float) {
        if (boosting) {
            if (freeBoostRemaining > 0f) {
                // TURBO_START: ilk saniyelerde enerji harcanmaz.
                freeBoostRemaining -= dt
            } else {
                boost =
                    (boost - UpgradeCatalog.boostDrain(upgrades.boost, car) * dt).coerceAtLeast(0f)
                if (boost <= 0f) boostLockedUntilRelease = true
            }
            addParticles(playerX, playerY + 86f, boostTrail = true)
        } else if (boostDown && boostLockedUntilRelease) {
            // Bos bar, parmak hala basili: ne calisir ne dolar. Oyuncu boost'u
            // birakmadan bar dolsaydi, birakip basmadan tekrar tutusurdu.
        } else {
            val regen = if (brakeDown) {
                GameConfig.BOOST_REGEN_PER_SEC_BRAKING
            } else {
                GameConfig.BOOST_REGEN_PER_SEC
            }
            boost = (boost + regen * dt).coerceAtMost(GameConfig.BOOST_MAX)
        }
    }

    private fun updateCombo(dt: Float) {
        if (combo > 0) {
            comboTimer -= dt
            if (comboTimer <= 0f) {
                combo = 0
                events.add(GameEvent.ComboBroken)
            }
        }
    }

    private fun spawn(dt: Float) {
        val traffic = endlessTrafficMultiplier()
        if (spawnPausedFor > 0f) {
            // Revive sonrasi guvenli pencere: coin dogmaya devam eder (tehdit
            // degil), arac dogmaz. Sayac da ilerlemez ki pencere bitince
            // birikmis bir arac aniden dusmesin.
            spawnPausedFor -= dt
            coinSpawnAcc += dt
            if (coinSpawnAcc > GameConfig.COIN_SPAWN_INTERVAL_SEC) {
                spawnCoin()
                coinSpawnAcc = 0f
            }
            return
        }
        obstacleSpawnAcc += dt
        coinSpawnAcc += dt
        if (obstacleSpawnAcc > obstacleSpawnInterval(traffic)) {
            spawnObstacle()
            obstacleSpawnAcc = 0f
        }
        if (coinSpawnAcc > GameConfig.COIN_SPAWN_INTERVAL_SEC) {
            spawnCoin()
            coinSpawnAcc = 0f
        }
    }

    private fun spawnObstacle() {
        val lane = random.nextInt(GameConfig.LANE_COUNT)
        obstacles.add(
            Obstacle(
                lane = lane,
                x = laneCenter(lane),
                // Dogma yuksekligi SINIFA gore: sabit -150 binek icin
                // dogruydu ama AGIR 161.6 px uzun, yani tir orada dogsa
                // arkasi ekranin ICINDE belirirdi (2026-08-16).
                y = GameConfig.obstacleSpawnY(CarCatalog.trafficShape.vehicleClass),
                colorIndex = random.nextInt(OBSTACLE_COLORS.size),
                ownSpeed = randomTrafficSpeed(),
                // Trafigin kutusu KENDI govdesinden gelir. Bugun tek bir
                // trafik govdesi var ve o BINEK; birden fazla govde/sinif
                // dogmaya baslarsa degisecek tek yer burasidir.
                vehicleClass = CarCatalog.trafficShape.vehicleClass
            )
        )
    }

    /**
     * Yeni bir trafik aracinin kendi hizi. Oyuncunun ANLIK hizina degil
     * kosunun TABAN hizina baglidir — boost'a basinca oyuncu gercekten daha
     * hizli yaklassin diye (bkz. [GameConfig.TRAFFIC_SPEED_RATIO_MIN]).
     */
    private fun randomTrafficSpeed(): Float {
        val ratio = GameConfig.TRAFFIC_SPEED_RATIO_MIN +
            random.nextFloat() *
            (GameConfig.TRAFFIC_SPEED_RATIO_MAX - GameConfig.TRAFFIC_SPEED_RATIO_MIN)
        return baseSpeed * ratio
    }

    /**
     * O anki engel dogma araligi. Iki carpan var:
     * - [endlessTrafficMultiplier]: sonsuz modda sure ilerledikce siklasir.
     * - [LevelDef.trafficDensity]: bolumun kendi yogunlugu (varsayilan 1.0).
     *   Ilk bolumler bunu 1'in ALTINDA tutar; yol seyrek, oyuncu once serit
     *   degistirmeyi ogrenir.
     */
    fun obstacleSpawnInterval(endlessTraffic: Float = endlessTrafficMultiplier()): Float {
        val density = level?.trafficDensity ?: 1f
        return GameConfig.OBSTACLE_SPAWN_INTERVAL_SEC / (endlessTraffic * density)
    }

    private fun spawnCoin() {
        val lane = random.nextInt(GameConfig.LANE_COUNT)
        coins.add(
            Coin(
                lane = lane,
                x = laneCenter(lane),
                y = GameConfig.COIN_SPAWN_Y_PX,
                pulse = random.nextFloat() * 6.28f
            )
        )
    }

    private fun updateObstacles(dt: Float) {
        // Carpisma kutusu cizimden turetilir (gorunmeyen bir alana carpip
        // "haksiz kaza" hissi olusmasin diye) ve cizim de ARACIN OLCU
        // SINIFINDAN gelir: oyuncununki kendi govdesinin sinifindan
        // ([CarShapeDef.vehicleClass]), her engelinki kendi sinifindan
        // ([Obstacle.vehicleClass]).
        //
        // Tek sabit kutu neden yetmiyor (docs/VEHICLE_CLASSES.md §2): 22
        // birimlik motosiklet 40 birimlik binek kutusuyla carpsaydi oyuncu
        // "yanimdaki bosluga carptim" derdi; 202 birimlik tir ise kendi
        // boyunun ucte birinden kisa bir kutuyla, yani "icinden gecilebilir"
        // olurdu. BINEK sinifi CIPA: bu blok binek-binek ciftinde bit
        // bazinda eski degerleri uretir (bkz. HitboxSpec).
        val playerBox = hitboxOf(car.vehicleClass)
        val playerHitLeft = playerX - playerBox.width / 2f
        val playerHitTop = playerY + playerBox.hitTop
        val playerHitBottom = playerHitTop + playerBox.height

        var i = obstacles.size - 1
        while (i >= 0) {
            val o = obstacles[i]
            val obstacleBox = hitboxOf(o.vehicleClass)
            // Ekrandaki asagi hiz = YAKLASMA hizi. Zemin `speed * K` ile
            // kaydigi icin aradaki fark kadar arac asfalta gore ILERI gider.
            // Fark negatife donerse (oyuncu frende, trafigin altinda) arac
            // yukari dogru uzaklasir; asagidaki ust sinir onu temizler.
            o.y += (speed - o.ownSpeed) * GameConfig.WORLD_PX_PER_SPEED_UNIT * dt

            val obstacleHitLeft = o.x - obstacleBox.width / 2f
            val obstacleHitTop = o.y + obstacleBox.hitTop
            val verticallyOverlapping =
                playerHitTop < obstacleHitTop + obstacleBox.height &&
                    playerHitBottom > obstacleHitTop

            if (verticallyOverlapping) {
                val dx = abs(playerX - o.x)
                if (dx < o.minDx) o.minDx = dx

                val hit = rectHit(
                    playerHitLeft, playerHitTop, playerBox.width, playerBox.height,
                    obstacleHitLeft, obstacleHitTop, obstacleBox.width, obstacleBox.height
                )
                if (hit && invulnerableFor <= 0f) {
                    if (secondChanceAvailable) {
                        secondChanceAvailable = false
                        invulnerableFor = GameConfig.INVULNERABLE_SEC_AFTER_SAVE
                        obstacles.removeAt(i)
                        events.add(GameEvent.Crash(saved = true))
                        i--
                        continue
                    }
                    onCrash()
                    return
                }
            }

            // Oyuncuyu geride biraktigi an: gecis puani + perfect dodge kontrolu.
            //
            // Gecis cizgisi: engelin BURNU (cizim ust kenari) oyuncunun arka
            // tamponunun altina indiginde arac tamamen geride kalmistir.
            // Engel asagi dogru aktigi icin en son GECEN kenari burnudur —
            // uzun bir govdenin kuyrugu cok once gecmis olur, o yuzden pay
            // engelin BOYUNDAN degil burun payindan hesaplanir.
            //
            // Pay tek bir float olarak ONCE hesaplaniyor: binek-binek
            // ciftinde `artBottom - artTop` tam olarak
            // [GameConfig.CAR_HEIGHT_PX] eder ve eski
            // `playerY + CAR_HEIGHT_PX` esigi bit bazinda korunur. Terimleri
            // ayri ayri eklemek son bitte 1 ulp kaydiriyordu.
            val passLine = playerY + (playerBox.artBottom - obstacleBox.artTop)
            if (!o.evaluated && o.y > passLine) {
                o.evaluated = true
                vehiclesPassed++
                score += GameConfig.SCORE_PER_PASSED_VEHICLE
                events.add(GameEvent.VehiclePassed)
                // Esik hem serit araligina hem ARAC CIFTINE bagli.
                if (o.minDx <= perfectDodgeMaxDx(obstacleBox)) registerPerfectDodge()
            }

            // Iki yonlu temizlik. Ust sinir, oyuncu frene basip trafigin
            // altina dustugu (yaklasma hizi negatif) durum icin: arac yukari
            // dogru uzaklasir ve bir daha geri gelmez. `evaluated` bayragi
            // korundugu icin geri gelse bile ikinci kez puan/dodge vermez.
            val goneBelow = o.y > viewHeight + GameConfig.OBSTACLE_DESPAWN_MARGIN_PX
            // Ust sinir da SINIFA gore: uzun arac daha yukarida dogdugu
            // icin sabit esik ona daha az pay birakirdi.
            val goneAbove = o.y <
                GameConfig.obstacleSpawnY(o.vehicleClass) -
                GameConfig.OBSTACLE_DESPAWN_TOP_MARGIN_PX
            if (goneBelow || goneAbove) {
                obstacles.removeAt(i)
            }
            i--
        }
    }

    /**
     * Bu ARAC CIFTI icin perfect dodge esigi.
     *
     * [GameConfig.perfectDodgeMaxDx] tek kutulu dunyadan kaliyor: esigi
     * `CAR_WIDTH_PX + (serit - CAR_WIDTH_PX) * oran` diye kuruyor, yani taban
     * terimi BINEK'in gorunur genisligi. Sinifli dunyada dogru taban terim
     * CIFTIN yari genisligidir, `(gorselA + gorselB) / 2` — bkz.
     * `docs/VEHICLE_CLASSES.md` §2 "Perfect dodge penceresi bozulur".
     *
     * Duzeltilmezse ne olur (360 dp, serit 67.2, esik 49.6): binek oyuncunun
     * penceresi `(28.16 , 49.6]` = 21.44 dp. Motosiklet oyuncunun carpma
     * esigi 21.83'e duser ama esik 49.6'da kalir → pencere 27.78 dp, yani
     * **%30 daha genis**. Dar arac hem daha az carpar hem daha cok combo
     * yapardi; bilesik ve istenmeyen bir avantaj. Duzeltmeyle motosikletin
     * penceresi 20.58 dp (binege gore %-4), tir trafiginde 21.82 dp (%+1.8).
     *
     * Duzeltme neden FARK olarak ekleniyor: pencere orani ve serit
     * bagimliligi [GameConfig]'te TEK yerde kalsin diye. Binek-binek
     * ciftinde fark tam olarak `0f`, yani bugunku esik bit bazinda degismez.
     *
     * DIKKAT: [GameConfig.perfectDodgeMaxDx] gun gelip cift yari-genisligini
     * parametre olarak alirsa (belgedeki oneri) buradaki duzeltme
     * SILINMELIDIR — yoksa iki kez uygulanir.
     */
    private fun perfectDodgeMaxDx(obstacleBox: HitboxSpec): Float {
        val pairHalfWidth = (hitboxOf(car.vehicleClass).artWidth + obstacleBox.artWidth) / 2f
        return GameConfig.perfectDodgeMaxDx(laneWidth) + (pairHalfWidth - GameConfig.CAR_WIDTH_PX)
    }

    private fun registerPerfectDodge() {
        combo++
        comboTimer = GameConfig.COMBO_WINDOW_SEC
        if (combo > bestCombo) bestCombo = combo
        // Haftalik gorev sayaci: her 5'e ULASILDIGI anda bir kez sayilir
        // (combo 6, 7 ... ayni zinciri ikinci kez saymaz).
        if (combo == 5) bigCombos++
        perfectDodges++
        val multiplier = GameConfig.comboMultiplier(combo)
        val bonus = (GameConfig.PERFECT_DODGE_BASE_SCORE * multiplier).toInt()
        score += bonus * scoreMultiplier
        events.add(GameEvent.PerfectDodge(combo, multiplier, bonus))
    }

    private fun updateCoins(dt: Float) {
        var i = coins.size - 1
        while (i >= 0) {
            val c = coins[i]
            c.y += speed * GameConfig.WORLD_PX_PER_SPEED_UNIT * dt
            c.pulse += dt * 8f

            val pickedUp = abs(playerX - c.x) < 26f && abs((playerY + 36f) - c.y) < 38f
            if (pickedUp) {
                score += GameConfig.SCORE_PER_COIN * scoreMultiplier
                boost = (boost + GameConfig.BOOST_REFUND_PER_COIN).coerceAtMost(GameConfig.BOOST_MAX)
                coinsCollected += GameConfig.COINS_PER_PICKUP
                addParticles(c.x, c.y, boostTrail = false)
                coins.removeAt(i)
                events.add(GameEvent.CoinPicked(coinsCollected))
            } else if (c.y > viewHeight + GameConfig.COIN_DESPAWN_MARGIN_PX) {
                coins.removeAt(i)
            }
            i--
        }
    }

    private fun updateParticles(dt: Float) {
        var i = particles.size - 1
        while (i >= 0) {
            val p = particles[i]
            p.x += p.vx
            p.y += p.vy
            p.life -= dt
            p.vy += 0.03f
            if (p.life <= 0f) particles.removeAt(i)
            i--
        }
    }

    private fun addParticles(x: Float, y: Float, boostTrail: Boolean) {
        val count = if (boostTrail) 3 else 12
        repeat(count) { index ->
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (random.nextFloat() - 0.5f) * (if (boostTrail) 1.4f else 4.6f),
                    vy = if (boostTrail) {
                        2f + random.nextFloat() * 3f
                    } else {
                        (random.nextFloat() - 0.5f) * 4.6f
                    },
                    life = if (boostTrail) {
                        0.16f + random.nextFloat() * 0.12f
                    } else {
                        0.40f + random.nextFloat() * 0.20f
                    },
                    size = if (boostTrail) {
                        3f + random.nextFloat() * 3f
                    } else {
                        2f + random.nextFloat() * 3f
                    },
                    color = if (boostTrail) {
                        if (index % 2 == 1) COLOR_BOOST_BLUE else COLOR_BOOST_YELLOW
                    } else {
                        if (index % 2 == 1) COLOR_BOOST_YELLOW else COLOR_SPARK_ORANGE
                    }
                )
            )
        }
    }

    // -----------------------------------------------------------------
    // Bitis kosullari
    // -----------------------------------------------------------------

    private fun onCrash() {
        crashed = true
        score = (score - GameConfig.CRASH_SCORE_PENALTY).coerceAtLeast(0f)
        combo = 0
        boosting = false
        phase = RunPhase.CRASHED
        events.add(GameEvent.Crash(saved = false))
    }

    private fun onTimeLimitReached() {
        timeRemaining = 0f
        val goal = level?.goal
        // Sure hedefli bolumde sureyi doldurmak = basari; mesafe hedeflisinde
        // sure limitine takilmak = basarisizlik.
        val completed = goal is LevelGoal.SurviveTime
        finish(completed)
    }

    private fun checkGoalReached() {
        val goal = level?.goal
        if (goal is LevelGoal.ReachDistance && distanceMeters() >= goal.meters) {
            finish(completed = true)
            return
        }
        // Kariyer: TUM hedefler tutturulduysa bolum orada biter. Oyuncu
        // "uc gorevi de tamamladim ama bolum bitmedi" dedi (2026-08-15) —
        // hakliydi: bitis kosulu sure/mesafeydi, hedefler degil. Artik
        // yapacak bir sey kalmayinca bekletilmiyor.
        //
        // Sart: hedeflerin HEPSI yukari sayan turden olmali. "Bolumu
        // tamamla" (CompleteRun) ya da "frene en fazla N kez bas" gibi
        // hedefler kosu bitmeden degerlendirilemez; oyle bir hedef varsa
        // bolum yine kendi hedefiyle (sure/mesafe) biter.
        if (mode == RunMode.CAREER && level != null && level.stars.isNotEmpty()) {
            val stats = currentStats(completed = true)
            if (level.stars.all { it.autoCompletes && it.isMet(stats) }) {
                finish(completed = true)
                return
            }
        }

        // Gunluk gorev: EN UST kademe tutturuldugu anda kosu basariyla biter
        // (sadece yukari sayan hedefler icin — bkz. Objective.autoCompletes).
        // Ilk kademede bitirmek yanlis olurdu: oyuncu ayni kosuda ustteki
        // kademeleri de toplayabilmeli.
        if (mode == RunMode.DAILY && level != null) {
            val objective = level.stars.last()
            if (objective.autoCompletes && objective.isMet(currentStats(completed = true))) {
                finish(completed = true)
            }
        }
    }

    /**
     * Kosuyu bitirir. Carpisma sonrasi kullanici "devam" teklifini reddettiginde
     * de UI bunu cagirir.
     */
    fun finish(completed: Boolean = false) {
        if (phase == RunPhase.FINISHED) return
        phase = RunPhase.FINISHED
        boosting = false
        val stats = currentStats(completed = completed && !crashed)
        val stars = LevelEvaluator.stars(level, stats)
        // Gunluk gorevde kademe carpismadan bagimsiz sayilir (bkz. tiersReached).
        val dailyTiers = if (mode == RunMode.DAILY && level != null) {
            LevelEvaluator.tiersReached(level.stars, stats)
        } else {
            0
        }
        // Yildiz coini SADECE YENI kazanilan yildiz icin odenir. Eskiden her
        // oynayista tekrar odeniyordu ve bolum 1'i (30 sn, uc kolay yildiz)
        // tekrar tekrar oynamak, bolum 30'u oynamaktan daha karliydi — oyunun
        // en yuksek coin/saniye orani bir farm dongusuydu (2026-08-14).
        val newStars = (stars - previousStars).coerceAtLeast(0)
        var coinsEarned = stats.coinsCollected * GameConfig.COINS_PER_PICKUP +
            stats.score / GameConfig.SCORE_PER_BONUS_COIN +
            newStars * GameConfig.COINS_PER_STAR
        if (BoosterType.DOUBLE_REWARD in boosters) {
            coinsEarned *= GameConfig.DOUBLE_REWARD_MULTIPLIER
        }
        // Cok kisa kosu hic odemez: "basla - hemen birak - tekrar basla"
        // dongusu de bir farm yoluydu (bkz. GameConfig.MIN_PAID_RUN_SECONDS).
        if (stats.timeSurvivedSec < GameConfig.MIN_PAID_RUN_SECONDS) coinsEarned = 0
        val xpEarned = stats.score / GameConfig.XP_PER_SCORE_POINT_DIVISOR +
            stars * GameConfig.XP_PER_STAR

        val newRecord = mode == RunMode.ENDLESS && stats.timeSurvivedSec > endlessRecordSeconds
        val gap = endlessRecordSeconds - stats.timeSurvivedSec
        val secondsFromRecord = if (
            mode == RunMode.ENDLESS && !newRecord && gap in 1..GameConfig.NEAR_RECORD_SECONDS
        ) gap else null

        val result = RunResult(
            mode = mode,
            levelId = level?.id,
            stats = stats,
            stars = stars,
            newStars = newStars,
            // Kariyerde bolum, gorevlerin [GameConfig.MIN_STARS_TO_PASS]
            // kadari tamamlandiysa gecilir (uc gorevin ikisi). Ucuncusu
            // ustalik yildizidir; gerekcesi o sabitin basinda.
            passed = mode == RunMode.CAREER && level != null &&
                level.awardsStars && stars >= GameConfig.MIN_STARS_TO_PASS,
            coinsEarned = coinsEarned,
            xpEarned = xpEarned,
            dailyTiers = dailyTiers,
            newRecord = newRecord,
            secondsFromRecord = secondsFromRecord
        )
        lastResult = result
        events.add(GameEvent.Finished(result))
    }

    /** Carpisma aninda "reklamla devam" teklif edilebilir mi. */
    fun canRevive(): Boolean =
        phase == RunPhase.CRASHED && revivesUsed < GameConfig.REVIVE_MAX_PER_RUN

    // -----------------------------------------------------------------
    // Turetilmis degerler
    // -----------------------------------------------------------------

    fun distanceMeters(): Int = (distancePx / GameConfig.PIXELS_PER_METER).toInt()

    fun boostDistanceMeters(): Int = (boostDistancePx / GameConfig.PIXELS_PER_METER).toInt()

    fun speedKmh(): Int = GameConfig.speedToKmh(speed)

    fun currentStats(completed: Boolean = false): RunStats = RunStats(
        score = score.toInt(),
        timeSurvivedSec = timeElapsed.toInt(),
        distanceMeters = distanceMeters(),
        boostDistanceMeters = boostDistanceMeters(),
        vehiclesPassed = vehiclesPassed,
        perfectDodges = perfectDodges,
        bestCombo = bestCombo,
        bigCombos = bigCombos,
        coinsCollected = coinsCollected,
        brakeTaps = brakeTaps,
        crashed = crashed,
        revivesUsed = revivesUsed,
        completed = completed
    )

    /** Sonsuz modda sure ilerledikce hiz carpani (30sn -> +%10, 60sn -> +%20 ...). */
    fun endlessSpeedMultiplier(): Float {
        if (mode != RunMode.ENDLESS) return 1f
        val steps = floor(timeElapsed / GameConfig.ENDLESS_STEP_SECONDS)
        return (1f + GameConfig.ENDLESS_SPEED_STEP * steps)
            .coerceAtMost(GameConfig.ENDLESS_SPEED_MAX_MULTIPLIER)
    }

    private fun endlessTrafficMultiplier(): Float {
        if (mode != RunMode.ENDLESS) return 1f
        val steps = floor(timeElapsed / GameConfig.ENDLESS_STEP_SECONDS)
        return (1f + GameConfig.ENDLESS_TRAFFIC_STEP * steps)
            .coerceAtMost(GameConfig.ENDLESS_TRAFFIC_MAX_MULTIPLIER)
    }

    /**
     * Boost SU AN tutusabilir mi.
     *
     * false olmasinin iki sebebi var: bar bosaldi ve parmak hala basili
     * (kilit — bkz. [GameConfig.BOOST_REENGAGE_MIN]) ya da enerji yeniden
     * tutusma esiginin altinda. Ikisinde de oyuncu butona basar, hicbir sey
     * olmaz ve "boost yaptim ama saymadi" diye okur. Arayuz bu bayragi okuyup
     * butonu soluklastiriyor — kilit dogru davraniş, ama sessiz kalmasi degil.
     */
    fun isBoostReady(): Boolean =
        !boostLockedUntilRelease && (boost > GameConfig.BOOST_REENGAGE_MIN || freeBoostRemaining > 0f)

    /** Dokunulmazlik suresi devam ediyor mu (cizimde araci yanip sondurmek icin). */
    fun isInvulnerable(): Boolean = invulnerableFor > 0f

    private fun rectHit(
        ax: Float, ay: Float, aw: Float, ah: Float,
        bx: Float, by: Float, bw: Float, bh: Float
    ): Boolean = ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by

    companion object {
        /**
         * Sinif basina carpisma geometrisi, [VehicleClass.ordinal] ile
         * indekslenir. Kosu sirasinda tahsisat ve tekrar hesap olmasin diye
         * sinif KUMESI kadar (bugun 3) onceden kuruluyor.
         */
        private val HITBOXES: List<HitboxSpec> = VehicleClass.entries.map { HitboxSpec(it) }

        private fun hitboxOf(vehicleClass: VehicleClass): HitboxSpec =
            HITBOXES[vehicleClass.ordinal]

        /** Prototipteki engel renkleri: sari, mavi, beyaz, turuncu. */
        val OBSTACLE_COLORS = intArrayOf(0xFFFFD60A.toInt(), 0xFF00C2FF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF7B00.toInt())

        private val COLOR_BOOST_BLUE = 0xFF19A2FF.toInt()
        private val COLOR_BOOST_YELLOW = 0xFFFFD33D.toInt()
        private val COLOR_SPARK_ORANGE = 0xFFFF6536.toInt()
    }
}
