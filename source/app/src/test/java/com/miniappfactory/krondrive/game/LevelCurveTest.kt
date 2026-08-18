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
        // ANTRENMAN MODU KAPALI olcum yapiyoruz — acik kalirsa orta serit bos
        // olur, otopilot hic carpmaz ve "bu hedef ulasilabilir" sonucu YALAN
        // olur (bkz. GameEngine.sideLanesOnly).
        val e = GameEngine(
            mode = RunMode.CAREER,
            level = level,
            random = Random(seed),
            sideLanesOnly = false
        )
        // DIKKAT: `apply { setViewport(viewWidth, viewHeight) }` yazma —
        // isimler GameEngine'in KENDI viewWidth/viewHeight alanlarina (0f)
        // baglanir ve setViewport sessizce hicbir sey yapmaz.
        e.setViewport(VIEW_WIDTH, VIEW_HEIGHT)
        return e
    }

    private fun GameEngine.autopilot(
        style: Style,
        maxFrames: Int = 30_000,
        /**
         * Her RUNNING karesinde, [step] CAGRILMADAN once cagrilir. Denge
         * olcumleri (ornegin serit doluluk dokumu) motoru kendi kopyasini
         * yazmadan ornekleyebilsin diye var; null ise hicbir maliyeti yok.
         */
        sample: ((GameEngine) -> Unit)? = null
    ): RunResult? {
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

            // ICINDEN GECILEBILIR MI — YERLESILEBILIR MI DEGIL (2026-08-19).
            //
            // [enterable] bir seride YERLESMENIN kosulu: 240 px ilerideki bir
            // arac orada KALMAYI riskli yapar. Ama seridin icinden gecerken
            // (uc seritli yolda bir uctan otekine giderken) arac yalnizca
            // ~0.2 s o seritte kalir; onemli olan tek sey o an dikey
            // ortusme olmamasi.
            //
            // Ayrimin somut sebebi: bolum 11'de olculdu (tohum 1234) —
            // oyuncu en solda, orta seritte hizasinda bir arac, ve {0,1}
            // seritlerini kapatan bir cift yaklasiyor. Yanindaki arac
            // gectiginde cift artik 240 px'in icindeydi, yani [enterable]
            // ORTA SERIDI HIC ACMIYORDU ve otopilot yerinde durup carpiyordu.
            // Oysa 215 px onundeki bir aracin altindan gecip saga devam etmek
            // tamamen guvenli — carpisma dikey ortusme ister.
            fun passable(lane: Int): Boolean = obstacles.none { o ->
                o.lane == lane &&
                    o.y > playerY - TRANSIT_CLEAR_PX &&
                    o.y < playerY + GameConfig.CAR_HEIGHT_PX
            }

            val myGap = gapAhead(playerLane)

            // Serit degistirme her zaman TEK serit: iki serit birden atlarken
            // arac ortadaki seridin x'inden gecer ve oradaki araca carpar.
            val neighbours = (playerLane - 1..playerLane + 1)
                .filter { it in 0 until GameConfig.LANE_COUNT }

            var desired = when {
                // 1) Kendi seridim tehlikeliyse en genis nefes alanina dogru
                //    TEK ADIM at.
                //
                //    Hedef TUM seritler arasindan secilir, yalnizca komsular
                //    arasindan degil (2026-08-19). Eskiden komsuluk yeterliydi
                //    cunku her dogus tek bir seridi kapatiyordu: en fazla bir
                //    serit kacmak gerekiyordu. Cift dogus
                //    ([GameConfig.doubleSpawnChance]) iki seridi ayni anda
                //    kapatabiliyor; en soldayken duvar {0,1} ise tek acik
                //    serit IKI adim otede kalir ve komsulukla sinirli
                //    otopilot yerinde durup carpar. Bu bir denge sorunu
                //    degil, OTOPILOTUN GORME SINIRIYDI — insan oyuncu iki kez
                //    kaydirir.
                myGap < DANGER_AHEAD_PX -> {
                    val best = (0 until GameConfig.LANE_COUNT)
                        .maxWithOrNull(
                            compareBy<Int> { gapAhead(it) }.thenBy { abs(it - playerLane) * -1 }
                        ) ?: playerLane
                    val step = when {
                        best < playerLane -> playerLane - 1
                        best > playerLane -> playerLane + 1
                        else -> playerLane
                    }
                    // Hedef seride YERLESILIYORSA tam pay, ARADAN
                    // GECILIYORSA yalnizca dikey ortusme aranir.
                    val ok = if (step == best) enterable(step) else passable(step)
                    if (step == playerLane || ok) step else playerLane
                }

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

            sample?.invoke(this)
            step(dt)
        }
        if (phase == RunPhase.CRASHED) finish(completed = false)
        return lastResult
    }

    private fun play(
        level: LevelDef,
        seed: Int,
        style: Style = Style.SAFE,
        sample: ((GameEngine) -> Unit)? = null
    ): RunResult =
        requireNotNull(engineFor(level, seed).autopilot(style, sample = sample)) {
            "bolum ${level.id} sonuc uretmedi"
        }

    private fun RunResult.describe(): String =
        "yildiz=$stars skor=${stats.score} gecis=${stats.vehiclesPassed} " +
            "coin=${stats.coinsCollected} dodge=${stats.perfectDodges} " +
            "combo=${stats.bestCombo} boostM=${stats.boostDistanceMeters} " +
            "mesafe=${stats.distanceMeters} sure=${stats.timeSurvivedSec} " +
            "tamam=${stats.completed} kaza=${stats.crashed}"

    // -----------------------------------------------------------------

    /**
     * Her [Objective.CoinsAtLeast] hedefi de ulasilabilir olmali.
     *
     * `PassVehicles` icin yazilan bekci (asagida) 2026-08-19'da eklendi ve
     * ayni gun ayni hatanin COIN hedeflerinde de durdugu olculdu:
     *
     *     bolum  8: hedef 22, gercek tavan 15  -> IMKANSIZ
     *     bolum 16: hedef 48, gercek tavan 44  -> IMKANSIZ
     *     bolum 22: hedef 54, gercek tavan 51  -> IMKANSIZ
     *     bolum 28: hedef 55, gercek tavan 55  -> pay yok
     *
     * Ilerleme tikanmiyordu ([GameConfig.MIN_STARS_TO_PASS] = 2) ama o
     * bolumlerde ucuncu yildiz KAZANILAMAZ durumdaydi — oyuncu ugrasip
     * nedenini ogrenemezdi. Tur bazinda ayri test yaziliyor cunku her hedef
     * turunun tavani farkli bir mekanizmadan geliyor.
     */
    @Test
    fun `her coin hedefi ulasilabilir`() {
        LevelCatalog.levels.forEach { level ->
            val hedef = level.stars
                .filterIsInstance<Objective.CoinsAtLeast>()
                .firstOrNull()?.coins ?: return@forEach
            val acikUclu = level.copy(
                stars = listOf(
                    Objective.PassVehicles(99_999),
                    Objective.ScoreAtLeast(9_999_999),
                    Objective.CoinsAtLeast(99_999)
                )
            )
            val tavan = SEEDS.minOf { play(acikUclu, it).stats.coinsCollected }
            assertTrue(
                "bolum ${level.id}: coin hedefi $hedef ama gercek tavan $tavan — " +
                    "hedef tavanin %85'ini asmamali",
                hedef <= tavan * 0.85f
            )
        }
    }

    /**
     * Her [Objective.PassVehicles] hedefi GERCEKTEN ulasilabilir olmali.
     *
     * NEDEN AYRI BIR TEST: diger testler "uc hedeften ikisi tutsun" diye
     * bakiyor, yani TEK BIR hedefin imkansiz olmasi fark edilmeden geciyordu.
     * Sahibi 2026-08-18'de oynarken buldu: *"4. bolumde surekli boosta bassan
     * bile 29 arac gecmen imkansiz"* ve *"3. bolum 6, 4. bolum 29, 5. bolum 18
     * — sacma olmus"*. Ikisi de dogruydu.
     *
     * Kok sebep: ayni gun dunya %40 yavaslatilinca hedefler koru korune x0.6
     * ile olceklenmisti. Olcum degil aritmetikti; oysa gecilen arac sayisi
     * hiza dogrusal bagli degil (bolumun suresi, trafik yogunlugu ve aracin
     * kendi hizi da giriyor). Hedefler artik OLCUMDEN turuyor.
     *
     * ## Tavan neden hedefler KAPATILARAK olculur
     *
     * [GameEngine.checkGoalReached] kariyerde TUM hedefler tutunca kosuyu
     * bitiriyor. Yani hedefi dusurmek kosuyu KISALTIYOR ve olculen gecis
     * sayisi da dusuyor — olcum kendi olctugu seye bagimli. Ilk denemede tam
     * bu tuzaga dusuldu: hedef 36 iken 36, 27 iken 27, 20 iken 24 olculdu.
     *
     * Bu yuzden tavan, hedefleri ULASILAMAZ degerlerle degistirerek olculur:
     * kosu o zaman bolumun kendi hedefiyle (sure/mesafe) biter ve gercek
     * tavani verir.
     *
     * Esik: hedef, tavanin **%80'ini gecmemeli**. Otopilot vasat bir oyuncu;
     * insan daha iyisini yapar, yani bu pay cimri degil.
     */
    @Test
    fun `her gecis hedefi ulasilabilir`() {
        LevelCatalog.levels.forEach { level ->
            val hedef = level.stars
                .filterIsInstance<Objective.PassVehicles>()
                .firstOrNull()?.count ?: return@forEach
            val acikUclu = level.copy(
                stars = listOf(
                    Objective.PassVehicles(99_999),
                    Objective.ScoreAtLeast(9_999_999),
                    Objective.CoinsAtLeast(99_999)
                )
            )
            val tavan = SEEDS.minOf { play(acikUclu, it).stats.vehiclesPassed }
            assertTrue(
                "bolum ${level.id}: gecis hedefi $hedef ama gercek tavan $tavan — " +
                    "hedef tavanin %80'ini asmamali",
                hedef <= tavan * 0.80f
            )
        }
    }

    /**
     * Gecis hedefleri bir ZORLUK EGRISI olusturmali: bolum ilerledikce hedef
     * genel olarak yukselmeli. Sahibi *"3. bolum 6, 4. bolum 29, 5. bolum 18
     * — sacma olmus"* dedi; o siçrayip dusen dizi bir egri degildi.
     *
     * Kucuk dususlere izin var (bir bolum gercekten daha kisa olabilir —
     * orn. 10. bolum mesafe hedefli ve komsularindan kisa), ama bir hedef
     * bir onceki bolumun hedefinin %85'inin altina inemez.
     */
    @Test
    fun `gecis hedefleri yukselen bir egri olusturur`() {
        var onceki = 0
        var oncekiId = 0
        LevelCatalog.levels.forEach { level ->
            val hedef = level.stars
                .filterIsInstance<Objective.PassVehicles>()
                .firstOrNull()?.count ?: return@forEach
            assertTrue(
                "bolum ${level.id} hedefi $hedef, bolum $oncekiId hedefi $onceki — " +
                    "egri geriye gidiyor",
                hedef >= onceki * 0.85f
            )
            onceki = hedef
            oncekiId = level.id
        }
    }

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

    /**
     * KARIYERIN TAMAMI BITIRILEBILIR OLMALI (2026-08-16).
     *
     * Ustteki test yalnizca ilk 8 bolume bakiyordu, yani oyunun ucte ikisi
     * hicbir zaman dogrulanmadi. Ag 30 bolume acilinca **14 bolumun**
     * ilerlemeyi tikadigi goruldu: 9'dan sonrasi temkinli oyuncu icin
     * bitirilemezdi. Sebep her seferinde ayniydi — PerfectDodge/Combo gibi
     * bir beceri hedefi ilk iki sirada duruyordu ve temkinli oyun otuz
     * bolumun HICBIRINDE tek bir dodge ya da combo yapmiyor.
     *
     * Bu test o durumun geri gelmesini engelliyor. Kirilirsa yeni bir bolum
     * ya da yeni bir hedef degeri kariyeri kesmis demektir.
     */
    @Test
    fun `otuz bolumun tamami temkinli oyunla gecilebilir`() {
        LevelCatalog.levels.forEach { level ->
            SEEDS.forEach { seed ->
                val r = play(level, seed)
                assertTrue(
                    "bolum ${level.id} (tohum $seed) temkinli oyunla " +
                        "${GameConfig.MIN_STARS_TO_PASS} yildiz vermedi, " +
                        "kariyer burada kesiliyor: ${r.describe()}",
                    r.stars >= GameConfig.MIN_STARS_TO_PASS
                )
            }
        }
    }

    /**
     * Beceri hedefleri (Combo) yalnizca UCUNCU sirada durabilir ve oyunun
     * odullendirdigi tavani ([GameConfig.COMBO_MULTIPLIERS] doyum noktasi)
     * asamaz. PerfectDodge hedefi katalogda HIC olmamali.
     */
    @Test
    fun `beceri hedefleri yalnizca ustalik yildizinda ve odul tavaninin altinda`() {
        val comboCap = GameConfig.COMBO_MULTIPLIERS.size
        LevelCatalog.levels.forEach { level ->
            level.stars.forEachIndexed { index, objective ->
                assertTrue(
                    "bolum ${level.id}: PerfectDodges hedefi kaldirildi, geri gelmemeli",
                    objective !is Objective.PerfectDodges
                )
                if (objective is Objective.ComboAtLeast) {
                    assertTrue(
                        "bolum ${level.id}: combo hedefi ${index + 1}. sirada — " +
                            "beceri hedefi yalnizca ustalik yildizinda (3. sira) olabilir",
                        index == level.stars.lastIndex
                    )
                    assertTrue(
                        "bolum ${level.id}: combo ${objective.combo} isteniyor ama oyun " +
                            "combo $comboCap'ten sonrasini odullendirmiyor",
                        objective.combo <= comboCap
                    )
                }
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

    /**
     * OTUZ BOLUMUN TAMAMI (2026-08-16).
     *
     * Bugune kadar olcum yalnizca ilk 8 bolumu oynatiyordu — yani oyunun
     * ucte ikisi bir kez bile oynatilmamisti. Uc ayri inceleme (urun,
     * oynanis, regresyon) 21/24/27/30. bolumlerin `FinishUnderSeconds`
     * hedeflerinin yukseltmesiz araçla ulasilamaz GORUNDUGUNU soyledi ama
     * hicbiri olcemedi, cunku ag oraya uzanmiyordu.
     *
     * Bu test bir sey DOGRULAMAZ (assert yok) — sayilari basar. Denge
     * kararlarinin dayanagi bu ciktidir; tahminle hedef yazmak yerine
     * buraya bakilir.
     */
    @Test
    fun `tam olcum dokumu — otuz bolum`() {
        println("bolum | yildiz | skor | gecis | coin | dodge | combo | boostM | mesafe | sure | tamam | kaza")
        LevelCatalog.levels.forEach { level ->
            // Ic tohum: tek tohumun sansi denge karari vermesin.
            val runs = SEEDS.take(3).map { play(level, it, Style.SAFE) }
            val stars = runs.map { it.stars }
            val median = runs.sortedBy { it.stats.score }[runs.size / 2]
            println(
                "%2d | yildiz=%s (ort %.1f) | %s".format(
                    level.id,
                    stars.joinToString("/"),
                    stars.average(),
                    median.describe()
                )
            )
        }
    }

    // -----------------------------------------------------------------
    // TRAFIK DESENI OLCUMU (2026-08-19)
    // -----------------------------------------------------------------

    /**
     * Oyuncunun ONUNDEKI tepki penceresinde ([OCCUPANCY_WINDOW_PX]) kac
     * seridin kapali oldugu. 0 = yol tamamen acik, 3 = cikis yok.
     *
     * Pencere neden ekranin tamami degil: ekranin en ustundeki bir arac
     * oyuncuyu ~4 saniye sonra ilgilendirir, yani "zorluk" degildir. Karar
     * penceresi otopilotun tehlike esigiyle ([DANGER_AHEAD_PX]) ayni
     * tutuldu ki olculen sey oyuncunun GERCEKTEN manevra yapmak zorunda
     * oldugu an olsun.
     */
    private fun GameEngine.lanesBlockedAhead(): Int =
        (0 until GameConfig.LANE_COUNT).count { lane ->
            obstacles.any { o ->
                o.lane == lane &&
                    o.y < playerY + GameConfig.CAR_HEIGHT_PX &&
                    playerY - o.y <= OCCUPANCY_WINDOW_PX
            }
        }

    /** Bir bolumun serit doluluk histogrami: [0..3] kapali serit orani (%). */
    private fun occupancy(level: LevelDef): FloatArray {
        val hist = LongArray(GameConfig.LANE_COUNT + 1)
        SEEDS.forEach { seed ->
            play(level, seed) { e -> hist[e.lanesBlockedAhead()]++ }
        }
        val total = hist.sum().coerceAtLeast(1L).toFloat()
        return FloatArray(hist.size) { hist[it] * 100f / total }
    }

    /**
     * OLCUM — assert yok, sayilari basar.
     *
     * Bu dokum 2026-08-19'da su bulguyla dogdu: bolum 10, 20 ve 30 SATIR
     * SATIR ayni sayilari veriyordu. Sebep [LevelDef.trafficDensity]'nin
     * 7. bolumden sonra hep 1.0 olmasi ve doguslarin serit secimini
     * BAGIMSIZ rastgele yapmasiydi — yogunluk doyunca desen de sabitleniyor,
     * geriye yalnizca hedef rakamlarinin buyumesi kaliyordu.
     */
    @Test
    fun `serit doluluk dokumu`() {
        println("bolum | 0 dolu | 1 dolu | 2 dolu | 3 dolu | tikanik(>=2)")
        LevelCatalog.levels.forEach { level ->
            val o = occupancy(level)
            println(
                "%2d | %5.1f%% | %5.1f%% | %5.1f%% | %5.1f%% | %5.1f%%".format(
                    level.id, o[0], o[1], o[2], o[3], o[2] + o[3]
                )
            )
        }
    }

    /**
     * GEC BOLUMLER BIRBIRINDEN FARKLI OLMALI.
     *
     * Bolum 10/20/30 desen olarak BIREBIR ayniydi (olculdu 2026-08-19).
     * Bu test o durumun geri gelmesini engelliyor: tikanma orani
     * (>=2 serit kapali) bolumle birlikte gercekten yukselmeli.
     */
    @Test
    fun `gec bolumlerde trafik deseni yogunlasir`() {
        val blocked = listOf(10, 20, 30).map { id ->
            val o = occupancy(LevelCatalog.level(id)!!)
            id to (o[2] + o[3])
        }
        val text = blocked.joinToString(", ") { "bolum ${it.first}=%.1f%%".format(it.second) }
        assertTrue(
            "bolum 20 bolum 10'dan daha tikanik olmali — $text",
            blocked[1].second > blocked[0].second + 2f
        )
        assertTrue(
            "bolum 30 bolum 20'den daha tikanik olmali — $text",
            blocked[2].second > blocked[1].second + 2f
        )
    }

    private companion object {
        /** Yaygin bir telefon: 360 x 800 dp. Uzun ekran = uzun yaklasma mesafesi. */
        const val VIEW_WIDTH = 360f
        const val VIEW_HEIGHT = 800f

        /** Serit doluluk olcumunun karar penceresi (bkz. lanesBlockedAhead). */
        const val OCCUPANCY_WINDOW_PX = 420f

        /** Otopilotun tehlikeli saydigi dikey mesafe. */
        const val DANGER_AHEAD_PX = 420f
        const val COIN_LOOKAHEAD_PX = 300f
        const val BOOST_SAFE_PX = 520f

        /** Yan seride kayarken "omuz hizasi" sayilan dikey pencere. */
        const val SIDE_SWIPE_PX = 240f

        /**
         * ARADAN GECERKEN aranan bosluk. Bir arac boyunun ~1.5 kati; en
         * hizli yaklasmada bile serit degistirme suresinden (~0.19 s, bkz.
         * [GameConfig.LANE_LERP_RATE]) uzun bir pay birakir.
         */
        const val TRANSIT_CLEAR_PX = 95f

        /** Yanasma manevrasinin gecerli oldugu dikey pencere (carpisma kutusu boyu). */
        const val GRAZE_WINDOW_PX = 52f

        /** Dodge esiginin bu kadar altina inilince geri cekilinir. */
        const val GRAZE_MARGIN_PX = 3f

        val LEARNING_LEVELS: List<LevelDef> get() = LevelCatalog.levels.take(8)

        /** Rastgelelik hep sabit tohumlu; tek tohum sansli olmasin diye bes tane. */
        val SEEDS = listOf(1, 7, 42, 1234, 90210)
    }
}
