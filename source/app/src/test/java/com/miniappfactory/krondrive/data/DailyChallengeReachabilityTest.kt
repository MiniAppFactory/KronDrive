package com.miniappfactory.krondrive.data

import com.miniappfactory.krondrive.game.GameConfig
import com.miniappfactory.krondrive.game.GameEngine
import com.miniappfactory.krondrive.game.LevelDef
import com.miniappfactory.krondrive.game.LevelEvaluator
import com.miniappfactory.krondrive.game.Objective
import com.miniappfactory.krondrive.game.RunMode
import com.miniappfactory.krondrive.game.RunPhase
import com.miniappfactory.krondrive.game.RunResult
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * GUNLUK GOREVLERIN ULASILABILIRLIK TESTLERI (2026-08-19).
 *
 * `DailyChallengeGeneratorTest` gunluk gorevin BICIMSEL kurallarina bakar
 * (uc kademe, artan hedef, artan odul, `autoCompletes`). Burada gorev
 * gercekten OYNANIR.
 *
 * ## Neden bu dosya gerekti
 *
 * Kariyer tarafinda ayni ag (`LevelCurveTest`) 2026-08-16'dan beri var ve
 * beceri hedeflerinin ilerlemeyi kesmesini engelliyor. Gunluk gorevde boyle
 * bir ag YOKTU — once `dodge` sablonu (2026-08-17), sonra `combo` sablonu
 * (2026-08-19) temkinli oyuncuya GUNUN TAMAMINI kapatmis halde bulundu.
 * Ikisi de elle fark edildi, testle degil; ikincisi birincinin uzerinden
 * iki gun gecmeden ayni dosyada duruyordu.
 *
 * ## Neden gunlukteki risk kariyerdekinden BUYUK
 *
 * Kariyerde ulasilamaz bir beceri hedefi yalnizca 3. yildizi (ustalik)
 * kaciriyor; bolum [GameConfig.MIN_STARS_TO_PASS] = 2 ile yine geciliyor.
 * Gunlukte ise sablon gun kimliginin hash'i ile seciliyor ve kademeler
 * SIRALI degerlendiriliyor ([LevelEvaluator.tiersReached] ilk tutmayan
 * hedefte duruyor): ulasilamaz bir 1. kademe, o gun gelen oyuncuya gunun
 * 500 coin'inin SIFIRINI verir. Telafi yok, ertesi gunu beklemek var.
 *
 * ## Otopilot
 *
 * Desen `LevelCurveTest` ile ayni ve BILEREK kopyalandi: o dosyadaki olcum
 * kodu `game` paketinde ve private. Iki test dosyasini birbirine gorunur
 * kilmak yerine olcum burada bagimsiz duruyor.
 *
 * - [Style.SAFE]: temkinli oyuncu, hic risk almaz, neredeyse hic perfect
 *   dodge yapmaz. Urun karari bu profile gore verilir.
 * - [Style.RISKY]: yan araca bilerek yanasip son anda cekilen oyuncu.
 *   Yalnizca "matematiksel olarak imkansiz degil" demek icin — her karede
 *   pikselden mesafe okuyan kapali cevrimli bir denetleyici oldugu icin
 *   INSAN ELIYLE ulasilabilirligin kaniti DEGILDIR (bkz. `LevelCurveTest`
 *   ayni uyari).
 */
class DailyChallengeReachabilityTest {

    private val dt = 0.016f

    private enum class Style { SAFE, RISKY }

    private fun engineFor(level: LevelDef, seed: Int): GameEngine {
        // ANTRENMAN MODU KAPALI: `sideLanesOnly = true` iken orta serit bos
        // kalir, otopilot hic carpmaz ve "bu hedef ulasilabilir" sonucu
        // YALAN olur.
        val e = GameEngine(
            mode = RunMode.DAILY,
            level = level,
            random = Random(seed),
            sideLanesOnly = false
        )
        // DIKKAT: `apply { setViewport(viewWidth, viewHeight) }` yazma —
        // isimler GameEngine'in kendi (0f) alanlarina baglanir ve cagri
        // sessizce hicbir sey yapmaz.
        e.setViewport(VIEW_WIDTH, VIEW_HEIGHT)
        return e
    }

    private fun GameEngine.autopilot(style: Style, maxFrames: Int = 30_000): RunResult? {
        var guard = 0
        while (phase == RunPhase.COUNTDOWN && guard++ < 1000) step(dt)

        val dodgeCeiling = GameConfig.perfectDodgeMaxDx(laneWidth)
        guard = 0
        while (phase == RunPhase.RUNNING && guard++ < maxFrames) {
            // Bir seritteki en yakin tehdide olan dikey mesafe; gecilmis
            // araclar sayilmaz.
            fun gapAhead(lane: Int): Float = obstacles
                .filter { it.lane == lane && it.y < playerY + GameConfig.CAR_HEIGHT_PX }
                .minOfOrNull { playerY - it.y } ?: Float.MAX_VALUE

            // Omuz hizasinda arac varken yan serit "girilebilir" degildir —
            // insan oyuncu da oraya donmez.
            fun enterable(lane: Int): Boolean = obstacles.none { o ->
                o.lane == lane &&
                    o.y > playerY - SIDE_SWIPE_PX &&
                    o.y < playerY + GameConfig.CAR_HEIGHT_PX
            }

            val myGap = gapAhead(playerLane)
            // Serit degistirme her zaman TEK serit: iki serit atlarken arac
            // ortadaki seridin x'inden gecer ve oradaki araca carpar.
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

                // 2) Guvendeysem yakindaki bir coine yonel.
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
                // Yanasma manevrasi: yan seritte tam hizada bir arac varsa ona
                // yaslan, yatay mesafe dodge penceresine girince geri cek.
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

    /**
     * Kosu sonucu. Ayni (bolum, tohum, stil) uclusu deterministik oldugu icin
     * sonuc onbellege alinir — testler ayni kosuyu tekrar tekrar oynatmasin.
     */
    private fun play(level: LevelDef, seed: Int, style: Style = Style.SAFE): RunResult =
        RUN_CACHE.getOrPut(Triple(level, seed, style)) {
            requireNotNull(engineFor(level, seed).autopilot(style)) { "gunluk kosu sonuc uretmedi" }
        }

    /**
     * TAVAN olcumu icin kademeleri ulasilamaz degerlerle degistirir.
     *
     * Gerekce `LevelCurveTest` ile ayni: motor gunluk modda EN UST kademe
     * tutunca kosuyu bitiriyor (`GameEngine.checkGoalReached`). Gercek
     * hedeflerle olcmek "olctugu seye bagimli" bir sayi uretir — kosu erken
     * biter, tavan oldugundan dusuk gorunur.
     */
    private fun DailyChallenge.openEnded(): LevelDef = toLevelDef().copy(
        stars = listOf(
            Objective.PassVehicles(99_999),
            Objective.ScoreAtLeast(9_999_999),
            Objective.CoinsAtLeast(99_999)
        )
    )

    /** Bu kosuda kac kademe alindi (odul mantiginin kullandigi hesabin aynisi). */
    private fun tiersOf(challenge: DailyChallenge, seed: Int, style: Style): Int {
        val def = challenge.toLevelDef()
        return LevelEvaluator.tiersReached(def.stars, play(def, seed, style).stats)
    }

    private fun RunResult.describe(): String =
        "skor=${stats.score} gecis=${stats.vehiclesPassed} coin=${stats.coinsCollected} " +
            "dodge=${stats.perfectDodges} combo=${stats.bestCombo} " +
            "boostM=${stats.boostDistanceMeters} mesafe=${stats.distanceMeters} " +
            "sure=${stats.timeSurvivedSec} tamam=${stats.completed} kaza=${stats.crashed}"

    // -----------------------------------------------------------------

    /**
     * ASIL DEGISMEZ: hangi gun gelirse gelsin, temkinli oynayan bir oyuncu
     * gunluk gorevden EN AZ 1. kademeyi alabilmeli.
     *
     * Kirilirsa: eklenen/degistirilen sablon o gun gelen oyuncuya sifir coin
     * veriyor demektir. `combo` sablonu (kaldirildi, 2026-08-19) tam olarak
     * boyleydi — bes tohumun besinde 0 kademe.
     *
     * Esik bilerek "1. kademe": 3. kademe ASPIRASYONEL kalmali, yani bu test
     * gunluk gorevi kolaylastirmaya zorlamaz; yalnizca gunu SIFIRLAMASINI
     * yasaklar.
     */
    @Test
    fun `her gunluk sablonun ilk kademesi temkinli oyunla alinabilir`() {
        templates().forEach { challenge ->
            SEEDS.forEach { seed ->
                val reached = tiersOf(challenge, seed, Style.SAFE)
                val run = play(challenge.toLevelDef(), seed, Style.SAFE)
                assertTrue(
                    "gunluk sablon '${challenge.id}' (tohum $seed): temkinli oyun " +
                        "$reached kademe aldi — o gun gelen oyuncu " +
                        "${challenge.totalRewardCoins} coin'in sifirini alir. " +
                        "Hedefler=${challenge.tiers.map { it.objective.targetValue }}, " +
                        "kosu=${run.describe()}",
                    reached >= 1
                )
            }
        }
    }

    /**
     * KALDIRILAN `combo` SABLONUNUN OLCUMU CANLI TUTULUYOR (2026-08-19).
     *
     * Iki isi var:
     *
     * 1. Ustteki bekcinin DISI oldugunu gosterir: ayni yardimci
     *    ([tiersOf], [Style.SAFE]) kaldirilan sablonda 0 kademe uretiyor,
     *    yani bekci "her sablon >= 1" kuralini gercekten kirabiliyor. Aksi
     *    halde tum sablonlar 3/3 verdigi icin bekci hep yesil kalir ve
     *    bosuna duruyor mu diye anlasilmazdi.
     *
     * 2. Kaldirma kararinin dayanagini kodda tutar. Karar OLCUME dayaniyordu:
     *    180 sn'lik temkinli kosuda bes tohumun besinde `bestCombo = 0`.
     *
     * ⚠ BU TEST BIR GUN KIRILIRSA: temkinli oyun artik combo yapiyor demektir
     * (motor, trafik yogunlugu veya dodge penceresi degismistir). O zaman
     * karar YENIDEN DEGERLENDIRILMELI — sablonu geri getirmek anlamli olabilir.
     * Testi susturmak dogru cevap degil.
     */
    @Test
    fun `kaldirilan combo sablonu temkinli oyunla hala sifir kademe verir`() {
        val kaldirilan = DailyChallenge(
            id = "combo-kaldirildi",
            goal = templates().first().goal,
            tiers = listOf(
                DailyTier(Objective.ComboAtLeast(2), 80),
                DailyTier(Objective.ComboAtLeast(3), 140),
                DailyTier(Objective.ComboAtLeast(5), 280)
            )
        )
        SEEDS.forEach { seed ->
            val reached = tiersOf(kaldirilan, seed, Style.SAFE)
            assertTrue(
                "temkinli oyun (tohum $seed) combo sablonundan $reached kademe aldi — " +
                    "0 bekleniyordu. Combo artik temkinli oyunla ulasilabilirse " +
                    "DailyChallengeGenerator'daki kaldirma karari yeniden degerlendirilmeli: " +
                    "${play(kaldirilan.toLevelDef(), seed, Style.SAFE).describe()}",
                reached == 0
            )
        }
    }

    /**
     * Beceri hedefleri (Combo / PerfectDodges) gunluk kademelerde YALNIZCA
     * son sirada durabilir — kariyer katalogundaki kuralin aynisi
     * (`LevelCurveTest.beceri hedefleri yalnizca ustalik yildizinda...`).
     *
     * Bugun hicbir sablon boyle bir hedef TASIMIYOR: `dodge` 2026-08-17'de,
     * `combo` 2026-08-19'da kaldirildi (gerekceler
     * [DailyChallengeGenerator] icinde yazili). Bu test kuralin geri
     * gelmesini degil, YANLIS YERE geri gelmesini engelliyor: bir beceri
     * hedefi 1. veya 2. kademede durursa sirali degerlendirme yuzunden
     * ustundeki kademeler de kilitlenir ve gun sifirlanir.
     *
     * PerfectDodges ayrica HIC kullanilmamali: kariyerde de, haftalik
     * gorevlerde de kaldirildi.
     */
    @Test
    fun `beceri hedefi yalnizca son kademede ve odul tavaninin altinda`() {
        val comboCap = GameConfig.COMBO_MULTIPLIERS.size
        templates().forEach { challenge ->
            challenge.tiers.forEachIndexed { index, tier ->
                val objective = tier.objective
                assertTrue(
                    "gunluk sablon '${challenge.id}': PerfectDodges hedefi 2026-08-17'de " +
                        "kaldirildi, geri gelmemeli",
                    objective !is Objective.PerfectDodges
                )
                if (objective is Objective.ComboAtLeast) {
                    assertTrue(
                        "gunluk sablon '${challenge.id}': combo hedefi ${index + 1}. kademede — " +
                            "beceri hedefi yalnizca SON kademede olabilir, aksi halde " +
                            "ustundeki kademeler de kilitlenir",
                        index == challenge.tiers.lastIndex
                    )
                    assertTrue(
                        "gunluk sablon '${challenge.id}': combo ${objective.combo} isteniyor ama " +
                            "oyun combo $comboCap'ten sonrasini odullendirmiyor " +
                            "(GameConfig.COMBO_MULTIPLIERS)",
                        objective.combo <= comboCap
                    )
                }
            }
        }
    }

    /**
     * Olcum dokumu — assert yok, sayilari basar. Denge degistirirken tavanin
     * nereden geldigini gormek icin.
     *
     * Butun sablonlar ayni bolum tanimini (180 sn, varsayilan trafik) paylasir;
     * yalnizca kademeleri farklidir. Bu yuzden tavan BIR KEZ olculur.
     */
    @Test
    fun `olcum dokumu — gunluk sablonlar`() {
        val open = templates().first().openEnded()
        println("--- gunluk kosunun tavani (kademeler acik, bes tohum)")
        SEEDS.forEach { seed ->
            println("   TEMKINLI tohum $seed: ${play(open, seed, Style.SAFE).describe()}")
        }
        SEEDS.forEach { seed ->
            println("   RISKLI   tohum $seed: ${play(open, seed, Style.RISKY).describe()}")
        }
        println("--- sablonlarin aldigi kademe (0-3)")
        templates().forEach { challenge ->
            println(
                "   ${challenge.id.padEnd(9)} hedefler=${challenge.tiers.map { it.objective.targetValue }} " +
                    "TEMKINLI=${SEEDS.map { tiersOf(challenge, it, Style.SAFE) }} " +
                    "RISKLI=${SEEDS.map { tiersOf(challenge, it, Style.RISKY) }}"
            )
        }
    }

    private companion object {
        /** Yaygin bir telefon: 360 x 800 dp. */
        const val VIEW_WIDTH = 360f
        const val VIEW_HEIGHT = 800f

        /** Otopilotun tehlikeli saydigi dikey mesafe. */
        const val DANGER_AHEAD_PX = 420f
        const val COIN_LOOKAHEAD_PX = 300f
        const val BOOST_SAFE_PX = 520f

        /** Yan seride kayarken "omuz hizasi" sayilan dikey pencere. */
        const val SIDE_SWIPE_PX = 240f

        /** Yanasma manevrasinin gecerli oldugu dikey pencere. */
        const val GRAZE_WINDOW_PX = 52f

        /** Dodge esiginin bu kadar altina inilince geri cekilinir. */
        const val GRAZE_MARGIN_PX = 3f

        /** Rastgelelik hep sabit tohumlu; tek tohum sansli olmasin diye bes tane. */
        val SEEDS = listOf(1, 7, 42, 1234, 90210)

        /** (bolum, tohum, stil) -> sonuc. Kosular deterministik, tekrar oynatma. */
        val RUN_CACHE = HashMap<Triple<LevelDef, Int, Style>, RunResult>()

        /**
         * Uretilen TUM sablonlar. Generator listesi private; gorevler gun
         * kimliginden turedigi icin genis bir takvim taramasi butun sablonlari
         * kapsar (mevcut `DailyChallengeGeneratorTest` de ayni yolu kullaniyor).
         */
        val ALL_TEMPLATES: List<DailyChallenge> by lazy {
            val seen = LinkedHashMap<String, DailyChallenge>()
            for (year in 2026..2030) {
                for (month in 1..12) {
                    for (day in 1..28) {
                        val c = DailyChallengeGenerator
                            .forDay("%04d-%02d-%02d".format(year, month, day))
                        seen.putIfAbsent(c.id, c)
                    }
                }
            }
            seen.values.toList()
        }

        fun templates(): List<DailyChallenge> = ALL_TEMPLATES
    }
}
