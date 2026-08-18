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

    /**
     * Otopilotun "eli". Stil NE yapmak istedigini, bu iki alan da onu NE KADAR
     * IYI yapabildigini soyler.
     *
     * Neden gerekti (2026-08-19 olcumu): [Style.SAFE] otopilot 180 saniyelik
     * kosuda bes tohumun BESINDE hic kaza yapmiyor. Bu "temkinli oyuncu" degil,
     * her karede piksel okuyan MAKINE. Kademeleri onun tavanina gore ayarlamak,
     * kaldirilan `combo` sablonunun aynadaki hali olurdu: bu sefer 3. kademe
     * herkese kapanirdi. Insan oyuncuyu modellemek icin iki bozma yeterli:
     *
     * @param reactionFrames Karar iki karar arasinda DONDURULUR. 1 = her kare
     *   (makine). 60 Hz'de 8 kare ~ 130 ms, 12 kare ~ 200 ms: olculmus insan
     *   gorsel-motor tepki suresi araligi.
     * @param dangerPx Onunde bu kadar bosluk kalmadan tehlikeyi FARK ETMEZ.
     *   Kucuk deger = gec goren oyuncu.
     */
    private data class Pilot(
        val style: Style,
        val reactionFrames: Int = 1,
        val dangerPx: Float = DANGER_AHEAD_PX
    ) {
        override fun toString(): String =
            if (reactionFrames == 1 && dangerPx == DANGER_AHEAD_PX) style.name
            else "${style.name}(tepki=${reactionFrames}k, gorus=${dangerPx.toInt()}px)"
    }

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

    private fun GameEngine.autopilot(
        pilot: Pilot,
        maxFrames: Int = 30_000,
        onStep: (GameEngine) -> Unit = {}
    ): RunResult? {
        val style = pilot.style
        var guard = 0
        while (phase == RunPhase.COUNTDOWN && guard++ < 1000) step(dt)

        val dodgeCeiling = GameConfig.perfectDodgeMaxDx(laneWidth)
        // Iki karar arasinda TASINAN niyet. Insan da her karede yeniden karar
        // vermez; bir kez "sola geciyorum" der ve o karari uygular.
        var heldLane = playerLane
        var heldBoost = false
        var sinceDecision = Int.MAX_VALUE
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
                myGap < pilot.dangerPx ->
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
                            gapAhead(c.lane) > pilot.dangerPx
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

            // TEPKI SURESI: karar yalnizca [Pilot.reactionFrames] karede bir
            // TAZELENIR; aradaki karelerde bir onceki niyet uygulanir. Yukarida
            // hesaplanan `desired` bilerek atilir — hesabi her karede yapmak
            // ucuz, onemli olan ona gore DAVRANMAMAK.
            if (sinceDecision >= pilot.reactionFrames) {
                heldLane = desired
                heldBoost = myGap > BOOST_SAFE_PX && boost > GameConfig.BOOST_REENGAGE_MIN * 2f
                sinceDecision = 0
            }
            sinceDecision++

            if (heldLane < playerLane) steerLeft() else if (heldLane > playerLane) steerRight()
            // Boost: yol acikken bas, frene hic basma.
            setBoost(heldBoost)
            step(dt)
            onStep(this)
        }
        // `lastResult == null` demek kosu [maxFrames] ile KESILDI demek (yarim
        // kosu olcumu). Kaza gibi bitirilir; istatistikler o ana kadarkidir.
        if (phase == RunPhase.CRASHED || lastResult == null) finish(completed = false)
        return lastResult
    }

    /**
     * Kosu sonucu. Ayni (bolum, tohum, stil) uclusu deterministik oldugu icin
     * sonuc onbellege alinir — testler ayni kosuyu tekrar tekrar oynatmasin.
     */
    private fun play(level: LevelDef, seed: Int, pilot: Pilot): RunResult =
        RUN_CACHE.getOrPut(Triple(level, seed, pilot)) {
            requireNotNull(engineFor(level, seed).autopilot(pilot)) { "gunluk kosu sonuc uretmedi" }
        }

    private fun play(level: LevelDef, seed: Int, style: Style = Style.SAFE): RunResult =
        play(level, seed, Pilot(style))

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
     * UST KADEME BEDAVA OLMAMALI (2026-08-19).
     *
     * "1. kademe alinabilsin" bekcisinin SIMETRIGI. O bekci gunun sifirlanmasini
     * yasakliyor; bu bekci gunun BEDAVA olmasini yasakliyor. Ikisi birlikte
     * kademelere bir koridor ciziyor — tek basina biri, dengeyi karsi ucdan
     * bozmaya izin veriyordu ve nitekim bozulmustu: 2026-08-19 olcumunde alti
     * sablonun altisi da temkinli otopilotla 3/3 veriyordu.
     *
     * Kriter: kosunun YARISINDA (90 sn) kaza eden oyuncu 3. kademeyi ALMAMALI.
     * 3. kademe "kosuyu bitir" demek; yarida birakana verilirse aspirasyonel
     * olmaktan cikar.
     *
     * Olcum [openEnded] tanimi uzerinde yapilir: gercek kademelerle kosulsa
     * motor ust kademe tutunca kosuyu bitirir ve olcum kendi olctugu seye
     * bagimli hale gelirdi.
     */
    @Test
    fun `ucuncu kademe yarim kosuyla alinamaz`() {
        val open = templates().first().openEnded()
        val halfRunFrames = (90 / dt).toInt()
        SEEDS.forEach { seed ->
            val half = requireNotNull(
                engineFor(open, seed).autopilot(Pilot(Style.SAFE), maxFrames = halfRunFrames)
            )
            templates().forEach { challenge ->
                val reached = LevelEvaluator.tiersReached(challenge.toLevelDef().stars, half.stats)
                assertTrue(
                    "gunluk sablon '${challenge.id}' (tohum $seed): 90 sn'de biten kosu " +
                        "$reached kademe aldi — 3. kademe kosuyu BITIRMEYI istemeli. " +
                        "Hedefler=${challenge.tiers.map { it.objective.targetValue }}, " +
                        "yarim kosu=${half.describe()}",
                    reached < 3
                )
            }
        }
    }

    /**
     * ...ama 3. kademe IMKANSIZ da olmamali: kazasiz tamamlanan bir kosu
     * ucunu de vermeli.
     *
     * Ustteki bekciyle birlikte koridorun iki ucu da baglanmis olur. Bu test
     * kirilirsa kademeler fazla yukseltilmis demektir — `dodge`/`combo`
     * hatasinin ust kademedeki hali.
     */
    @Test
    fun `ucuncu kademe kazasiz tam kosuyla alinabilir`() {
        templates().forEach { challenge ->
            SEEDS.forEach { seed ->
                val reached = tiersOf(challenge, seed, Style.SAFE)
                assertTrue(
                    "gunluk sablon '${challenge.id}' (tohum $seed): kazasiz 180 sn'lik kosu " +
                        "yalnizca $reached kademe aldi — 3. kademe aspirasyonel olmali ama " +
                        "ULASILABILIR kalmali. " +
                        "Hedefler=${challenge.tiers.map { it.objective.targetValue }}, " +
                        "kosu=${play(challenge.toLevelDef(), seed, Style.SAFE).describe()}",
                    reached == 3
                )
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

    @Test
    fun `olcum dokumu — tepki suresi taramasi`() {
        val open = templates().first().openEnded()
        println("--- tepki suresi / gorus alani taramasi (SAFE stil, bes tohum ortalamasi)")
        listOf(1, 4, 6, 8, 10, 12, 14, 16, 20).forEach { frames ->
            listOf(DANGER_AHEAD_PX, 340f, 260f).forEach { danger ->
                val p = Pilot(Style.SAFE, frames, danger)
                val runs = SEEDS.map { play(open, it, p) }
                fun avg(f: (RunResult) -> Number) = runs.map { f(it).toDouble() }.average()
                println(
                    "   tepki=%2dk(%3dms) gorus=%3d | sure=%5.1f skor=%7.0f gecis=%5.1f coin=%5.1f boostM=%6.0f mesafe=%6.0f kaza=%d/5".format(
                        frames, (frames * 16), danger.toInt(),
                        avg { it.stats.timeSurvivedSec }, avg { it.stats.score },
                        avg { it.stats.vehiclesPassed }, avg { it.stats.coinsCollected },
                        avg { it.stats.boostDistanceMeters }, avg { it.stats.distanceMeters },
                        runs.count { it.stats.crashed }
                    )
                )
            }
        }
    }

    @Test
    fun `olcum dokumu — hayatta kalinan saniye basina kazanim`() {
        val open = templates().first().openEnded()
        val checkpoints = listOf(20, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180)
        println("--- kosunun ILERLEYISI (SAFE, bes tohum ortalamasi)")
        println("   sn  | skor   gecis  coin  boostM  mesafe")
        val rows = checkpoints.associateWith { ArrayList<com.miniappfactory.krondrive.game.RunStats>() }
        SEEDS.forEach { seed ->
            val remaining = checkpoints.toMutableList()
            engineFor(open, seed).autopilot(Pilot(Style.SAFE)) { e ->
                while (remaining.isNotEmpty() && e.currentStats().timeSurvivedSec >= remaining.first()) {
                    rows.getValue(remaining.removeAt(0)).add(e.currentStats())
                }
            }
        }
        checkpoints.forEach { sec ->
            val r = rows.getValue(sec)
            if (r.isEmpty()) return@forEach
            fun avg(f: (com.miniappfactory.krondrive.game.RunStats) -> Number) =
                r.map { f(it).toDouble() }.average()
            println(
                "   %3d | %6.0f %5.1f %6.1f %6.0f %7.0f".format(
                    sec, avg { it.score }, avg { it.vehiclesPassed }, avg { it.coinsCollected },
                    avg { it.boostDistanceMeters }, avg { it.distanceMeters }
                )
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
        val RUN_CACHE = HashMap<Triple<LevelDef, Int, Pilot>, RunResult>()

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
