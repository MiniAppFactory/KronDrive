package com.miniappfactory.krondrive.game

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Ogrenme egrisinin ULASILABILIRLIK testleri (2026-08-14).
 *
 * `LevelCatalogTest` bolum tanimlarinin bicimsel kurallarini dogrular;
 * burada bolumler gercekten OYNANIR. Iki otopilot motoru surer:
 *
 * - [Style.SAFE]: temkinli oyuncu. Serit degistirmeyi erken yapar, hic risk
 *   almaz, dolayisiyla neredeyse hic Perfect Dodge yapmaz.
 * - [Style.RISKY]: yan seritteki araca bilerek yanasip son anda geri cekilen
 *   oyuncu — Perfect Dodge/combo hedeflerinin insan eliyle tutturulabilir
 *   oldugunu dogrular.
 *
 * Ikisi de bilerek VASAT: mukemmel zamanlama yapmazlar. Otopilotun
 * basaramadigi bir hedef, yeni bir oyuncu icin de fazla zordur.
 *
 * En kritik degismez: **her bolum [GameConfig.MIN_STARS_TO_PASS] yildiz
 * verebilmeli** (su an 2), cunku bir sonraki bolum bununla aciliyor
 * (`GameStateRepository.recordLevelResult`). Ilk hedefi tutturamayan oyuncu
 * oyunda tikanir.
 */
class LevelCurveTest {

    private val dt = 0.016f

    private enum class Style { SAFE, RISKY }

    private fun engineFor(level: LevelDef, seed: Int): GameEngine {
        val e = GameEngine(mode = RunMode.CAREER, level = level, random = Random(seed))
        // DIKKAT: `apply { setViewport(viewWidth, viewHeight) }` yazma —
        // isimler GameEngine'in KENDI viewWidth/viewHeight alanlarina (0f)
        // baglanir ve setViewport sessizce hicbir sey yapmaz.
        e.setViewport(VIEW_WIDTH, VIEW_HEIGHT)
        return e
    }

    private fun GameEngine.autopilot(style: Style, maxFrames: Int = 30_000): RunResult? {
        var guard = 0
        while (phase == RunPhase.COUNTDOWN && guard++ < 1000) step(dt)

        val dodgeCeiling = GameConfig.perfectDodgeMaxDx(laneWidth)
        guard = 0
        while (phase == RunPhase.RUNNING && guard++ < maxFrames) {
            // Bir seritteki en yakin tehdide olan dikey mesafe. Gecilmis
            // araclar (oyuncunun altinda kalanlar) sayilmaz.
            fun gapAhead(lane: Int): Float = obstacles
                .filter { it.lane == lane && it.y < playerY + GameConfig.CAR_HEIGHT_PX }
                .minOfOrNull { playerY - it.y } ?: Float.MAX_VALUE

            // Yan serit ancak "girilebilir" ise secilir: oyuncunun HIZASINDA
            // bir arac varsa oraya kaymak yandan carpmak demektir. Insan
            // oyuncu da omuz hizasindaki araca donmez.
            fun enterable(lane: Int): Boolean = obstacles.none { o ->
                o.lane == lane &&
                    o.y > playerY - SIDE_SWIPE_PX &&
                    o.y < playerY + GameConfig.CAR_HEIGHT_PX
            }

            val myGap = gapAhead(playerLane)

            // Serit degistirme her zaman TEK serit: iki serit birden atlarken
            // arac ortadaki seridin x'inden gecer ve oradaki araca carpar.
            val neighbours = (playerLane - 1..playerLane + 1)
                .filter { it in 0 until GameConfig.LANE_COUNT }

            var desired = when {
                // 1) Kendi seridim tehlikeliyse en genis nefes alanina gec.
                myGap < DANGER_AHEAD_PX ->
                    neighbours
                        .filter { it == playerLane || enterable(it) }
                        .maxWithOrNull(
                            compareBy<Int> { gapAhead(it) }.thenBy { abs(it - playerLane) * -1 }
                        ) ?: playerLane

                // 2) Guvendeysem yakindaki bir coine yonel (yan serit yeterli).
                else -> coins
                    .filter { c ->
                        c.y > playerY - COIN_LOOKAHEAD_PX &&
                            c.y < playerY + 30f &&
                            c.lane in neighbours &&
                            enterable(c.lane) &&
                            gapAhead(c.lane) > DANGER_AHEAD_PX
                    }
                    .minByOrNull { abs(it.y - playerY) }
                    ?.lane
                    ?: playerLane
            }

            if (style == Style.RISKY) {
                // Yanasma manevrasi: yan seritte, dikey olarak TAM hizada bir
                // arac varsa ona dogru yaslan; yatay mesafe dodge penceresine
                // girdigi anda geri cek. Kapali dongu oldugu icin carpisma
                // sinirinin (hitbox genisligi) altina inmiyor.
                val neighbour = obstacles.firstOrNull { o ->
                    abs(o.lane - playerLane) == 1 &&
                        abs(o.y - playerY) < GRAZE_WINDOW_PX &&
                        gapAhead(playerLane) > DANGER_AHEAD_PX
                }
                if (neighbour != null) {
                    val dx = abs(playerX - neighbour.x)
                    desired = if (dx > dodgeCeiling - GRAZE_MARGIN_PX) neighbour.lane else playerLane
                }
            }

            if (desired < playerLane) steerLeft() else if (desired > playerLane) steerRight()

            // Boost: yol acikken bas, frene hic basma.
            setBoost(myGap > BOOST_SAFE_PX && boost > GameConfig.BOOST_REENGAGE_MIN * 2f)

            step(dt)
        }
        if (phase == RunPhase.CRASHED) finish(completed = false)
        return lastResult
    }

    private fun play(level: LevelDef, seed: Int, style: Style = Style.SAFE): RunResult =
        requireNotNull(engineFor(level, seed).autopilot(style)) {
            "bolum ${level.id} sonuc uretmedi"
        }

    private fun RunResult.describe(): String =
        "yildiz=$stars skor=${stats.score} gecis=${stats.vehiclesPassed} " +
            "coin=${stats.coinsCollected} dodge=${stats.perfectDodges} " +
            "combo=${stats.bestCombo} boostM=${stats.boostDistanceMeters} " +
            "mesafe=${stats.distanceMeters} sure=${stats.timeSurvivedSec} " +
            "tamam=${stats.completed} kaza=${stats.crashed}"

    // -----------------------------------------------------------------

    @Test
    fun `bolum 1 kaybedilmesi neredeyse imkansiz bir tanitim bolumudur`() {
        val level = LevelCatalog.levels[0]
        SEEDS.forEach { seed ->
            val r = play(level, seed)
            assertTrue("bolum 1 (tohum $seed) kaza ile bitti", !r.stats.crashed)
            assertTrue("bolum 1 (tohum $seed) tamamlanamadi", r.stats.completed)
            assertTrue("bolum 1 (tohum $seed) uc yildiz vermedi: ${r.describe()}", r.stars == 3)
        }
    }

    @Test
    fun `ilk sekiz bolum temkinli oyunla carpmadan tamamlanabilir`() {
        LEARNING_LEVELS.forEach { level ->
            SEEDS.forEach { seed ->
                val r = play(level, seed)
                assertTrue(
                    "bolum ${level.id} (tohum $seed) tamamlanamadi: ${r.describe()}",
                    r.stats.completed
                )
            }
        }
    }

    @Test
    fun `ilk sekiz bolumde ilerleme tikanmaz — gecis yildizi her zaman alinir`() {
        // Bu test 2026-08-16'da GUCLENDIRILDI ve neden onemli oldugu burada
        // dursun: esik `stars >= 1` yaziyordu, oysa kural 15 Agustos'ta
        // "ucu de" olarak degismisti. Test eski kurali dogruladigi icin
        // gecmeye devam etti ve ilk 8 bolumdeki tikanma FARK EDILMEDI
        // (bkz. docs/DIFFICULTY_REVIEW.md).
        //
        // Artik esik sabitten okunuyor: kural bir daha degisirse bu test
        // sessizce eskimek yerine kirilir.
        LEARNING_LEVELS.forEach { level ->
            SEEDS.forEach { seed ->
                val r = play(level, seed)
                assertTrue(
                    "bolum ${level.id} (tohum $seed) temkinli oyunla " +
                        "${GameConfig.MIN_STARS_TO_PASS} yildiz vermedi, " +
                        "ilerleme tikanir: ${r.describe()}",
                    r.stars >= GameConfig.MIN_STARS_TO_PASS
                )
            }
        }
    }

    @Test
    fun `ilk bes bolumde temkinli oyun iki yildizi da alir`() {
        // Ilk bes bolumun ilk IKI hedefi "katilim" seviyesinde; ucuncusu
        // bilerek optimizasyon hedefi (bkz. game-scenario).
        LevelCatalog.levels.take(5).forEach { level ->
            SEEDS.forEach { seed ->
                val r = play(level, seed)
                assertTrue(
                    "bolum ${level.id} (tohum $seed): ${r.describe()}",
                    r.stars >= 2
                )
            }
        }
    }

    @Test
    fun `risk alan oyun Perfect Dodge ve combo hedeflerine ulasir`() {
        // Bu test hedefin ULASILABILIR oldugunu gosterir, INSAN ELIYLE
        // ulasilabilir oldugunu DEGIL.
        //
        // Eski yorum "otopilot tutturabiliyorsa insan da tutturabilir"
        // diyordu. Bu cikarim GECERSIZ: RISKY profili her karede pikselden
        // mesafe okuyan kapali cevrimli bir denetleyici, yani insanustu.
        // Olcum (docs/DIFFICULTY_REVIEW.md, 2026-08-16): perfect dodge
        // penceresinden gecis 2-3 kare (33-67 ms) suruyor, insan tepki
        // tabani ~250 ms. Yani buradan gecen bir hedef yine de oyuncu icin
        // duvar olabilir. Testin isi yalnizca "matematiksel olarak
        // imkansiz degil" demek.
        //
        // Bolum 6 ve 7 artik dodge hedefi TASIMIYOR (ikisinde de 2. ve 3.
        // hedef birden beceri hedefiydi, 2026-08-16'da duzeltildi) — bu
        // yuzden dodge kontrolu kosullu.
        listOf(4, 6, 7).forEach { id ->
            val level = LevelCatalog.level(id)!!
            val needed = level.stars.filterIsInstance<Objective.PerfectDodges>()
                .maxOfOrNull { it.count }
            if (needed != null) {
                val best = SEEDS.map { play(level, it, Style.RISKY) }
                    .maxByOrNull { it.stats.perfectDodges }!!
                assertTrue(
                    "bolum $id: risk alan oyun $needed dodge yapamadi — ${best.describe()}",
                    best.stats.perfectDodges >= needed
                )
            }
            val combo = level.stars.filterIsInstance<Objective.ComboAtLeast>().maxOfOrNull { it.combo }
            if (combo != null) {
                val bestCombo = SEEDS.map { play(level, it, Style.RISKY) }.maxOf { it.stats.bestCombo }
                assertTrue(
                    "bolum $id: risk alan oyun ${combo}x combo yapamadi (en iyi $bestCombo)",
                    bestCombo >= combo
                )
            }
        }
    }

    @Test
    fun `olcum dokumu`() {
        // Denge degistirirken sayilarin nereden geldigini gormek icin.
        LEARNING_LEVELS.forEach { level ->
            val safe = play(level, SEEDS.first(), Style.SAFE)
            val risky = play(level, SEEDS.first(), Style.RISKY)
            println("bolum ${level.id} TEMKINLI: ${safe.describe()}")
            println("bolum ${level.id} RISKLI  : ${risky.describe()}")
        }
    }

    private companion object {
        /** Yaygin bir telefon: 360 x 800 dp. Uzun ekran = uzun yaklasma mesafesi. */
        const val VIEW_WIDTH = 360f
        const val VIEW_HEIGHT = 800f

        /** Otopilotun tehlikeli saydigi dikey mesafe. */
        const val DANGER_AHEAD_PX = 420f
        const val COIN_LOOKAHEAD_PX = 300f
        const val BOOST_SAFE_PX = 520f

        /** Yan seride kayarken "omuz hizasi" sayilan dikey pencere. */
        const val SIDE_SWIPE_PX = 240f

        /** Yanasma manevrasinin gecerli oldugu dikey pencere (carpisma kutusu boyu). */
        const val GRAZE_WINDOW_PX = 52f

        /** Dodge esiginin bu kadar altina inilince geri cekilinir. */
        const val GRAZE_MARGIN_PX = 3f

        val LEARNING_LEVELS: List<LevelDef> get() = LevelCatalog.levels.take(8)

        /** Rastgelelik hep sabit tohumlu; tek tohum sansli olmasin diye bes tane. */
        val SEEDS = listOf(1, 7, 42, 1234, 90210)
    }
}
