package com.miniappfactory.krondrive.data

import com.miniappfactory.krondrive.game.LevelDef
import com.miniappfactory.krondrive.game.LevelGoal
import com.miniappfactory.krondrive.game.Objective
import java.util.Calendar

/** Gunluk gorevin tek bir kademesi: hedef + o kademeye ulasinca verilen coin. */
data class DailyTier(val objective: Objective, val rewardCoins: Int)

/**
 * Gunun gorevi: tek bir kosuda ARTAN uc kademe.
 *
 * Neden tek odul degil de kademe (oyuncu istegi, 2026-08-14): tek ve yuksek
 * bir hedefte oyuncu ya hepsini alir ya hicbir seyi. Kademede ilk odul erken
 * gelir, ustu "biraz daha dayan" der; ayni kosuda ust kademeye cikilirsa
 * ARADAKI FARK degil, o kademenin odulu de eklenir.
 */
data class DailyChallenge(
    val id: String,
    val goal: LevelGoal,
    val tiers: List<DailyTier>
) {
    init {
        require(tiers.size == TIER_COUNT) { "Gunluk gorev $id: tam olarak $TIER_COUNT kademe olmali" }
    }

    /** Motorun anlayacagi bolum tanimi (kariyer yildizi vermez, kademe verir). */
    fun toLevelDef(): LevelDef = LevelDef(
        id = DAILY_LEVEL_ID,
        goal = goal,
        stars = tiers.map { it.objective },
        awardsStars = false
    )

    /** Tumu tamamlanirsa gunluk toplam kazanc. */
    val totalRewardCoins: Int get() = tiers.sumOf { it.rewardCoins }

    /**
     * Daha once [alreadyGranted] kademe odenmisken toplam [reached] kademeye
     * ulasildiginda odenecek coin. Yani `[alreadyGranted, reached)` araligi —
     * ayni kademe ikinci kez odenmez.
     */
    fun rewardBetween(alreadyGranted: Int, reached: Int): Int =
        tiers.withIndex()
            .filter { it.index in alreadyGranted until reached }
            .sumOf { it.value.rewardCoins }

    /** Kartta gosterilen baslik: ilgili kademenin hedefi. */
    fun title(language: AppLanguage, tierIndex: Int = tiers.lastIndex): String =
        tiers[tierIndex.coerceIn(0, tiers.lastIndex)].objective.label(language)

    companion object {
        /** Gunluk gorev, kariyer bolum numaralariyla cakismasin diye ayri bir id. */
        const val DAILY_LEVEL_ID = -1

        const val TIER_COUNT = 3
    }
}

/**
 * Gunluk gorev uretici. Gorev, tarihten TURETILIR — yani ayni gun uygulamayi
 * silip yeniden kursan bile ayni gorevi alirsin, sunucuya gerek yok.
 *
 * Yeni sablon eklemek icin listeye bir satir eklemek yeterli.
 */
object DailyChallengeGenerator {

    /** Sayarak ilerlenen gorevlerde kosunun en fazla surebilecegi sure. */
    private const val RUN_TIME_CAP_SEC = 180

    /**
     * Kademe odulleri her sablonda ayni: 80 / 140 / 280 = gunde en fazla 500.
     *
     * Once 200/400/800 (=1400) yapildi, ayni gun 900'e cekildi (2026-08-14),
     * **2026-08-16'da 500'e indirildi** (sahibi onayi; analiz
     * `docs/ECONOMY_BALANCE_PROPOSAL.md`).
     *
     * 900 neden fazlaydi: `docs/BALANCE.md` gunluk gorevi zaten "400-500 coin"
     * diye belgeliyordu — kod bir noktada 900'e cikmis, belge guncellenmemisti.
     * OLCULDU (`LevelCurveTest.olcum dokumu`): bir kariyer bolumunu ilk kez
     * gecmek ortalama **100 coin** odiyor. 900'lik gunluk, dokuz bolumluk
     * ilerlemeye bedeldi; oynamak yerine gunde bir kez girmek daha karliydi.
     * 500'de bu oran bese iner.
     *
     * Kademeli yapinin amaci korundu: 3. kademe hala en buyuk tek odul.
     *
     * GOC RISKI YOK: `DAILY_TIER` anahtari kademe SAYISINI sakliyor, coin
     * miktarini degil — odul her seferinde bu diziden yeniden hesaplaniyor.
     * Yani mevcut oyuncunun kayitli ilerlemesi bozulmaz.
     */
    private val TIER_REWARDS = intArrayOf(80, 140, 280)

    /** Ucu de ayni turden, artan hedefli bir gunluk gorev kurar. */
    private fun challenge(
        id: String,
        goal: LevelGoal = LevelGoal.SurviveTime(RUN_TIME_CAP_SEC),
        objectives: List<Objective>
    ): DailyChallenge {
        require(objectives.size == TIER_REWARDS.size) {
            "Gunluk gorev $id: ${TIER_REWARDS.size} hedef bekleniyor, ${objectives.size} verildi"
        }
        return DailyChallenge(
            id = id,
            goal = goal,
            tiers = objectives.mapIndexed { index, objective ->
                DailyTier(objective, TIER_REWARDS[index])
            }
        )
    }

    private val TEMPLATES: List<DailyChallenge> = listOf(
        // "dodge" gorevi KALDIRILDI (2026-08-17), yerine boost mesafesi.
        //
        // Ayni gun perfect dodge hedefleri 17 kariyer bolumunden ve haftalik
        // gorevlerden kaldirilmisti; gunluk sablonlar arasinda kalmis oldugu
        // gozden kacmisti. Olcum (LevelCurveTest): temkinli oyun otuz bolumun
        // HICBIRINDE tek bir dodge yapmiyor — yani bu gorev yedi gunde bir
        // gelip o oyuncuya gunun tamamini kapatiyordu.
        //
        // Mekanik duruyor ve hala skor + combo veriyor; SART olmaktan cikti.
        challenge(
            id = "boost",
            objectives = listOf(
                Objective.BoostDistance(350),
                Objective.BoostDistance(800),
                Objective.BoostDistance(1400)
            )
        ),
        challenge(
            id = "distance",
            objectives = listOf(
                Objective.DistanceAtLeast(1200),
                Objective.DistanceAtLeast(2600),
                Objective.DistanceAtLeast(4200)
            )
        ),
        // "combo" gorevi KALDIRILDI (2026-08-19) — `dodge` ile AYNI GEREKCE.
        //
        // Combo yalnizca [GameEngine.registerPerfectDodge] icinde artiyor,
        // yani `ComboAtLeast(n)` = "[GameConfig.COMBO_WINDOW_SEC] (6 sn)
        // icinde n mukemmel dodge zinciri". Dodge'dan STRIKT olarak daha zor:
        // dodge tek bir yakin gecis, combo(2) o gecisten IKISINI zincire
        // dizmek. Dodge 2026-08-17'de tam bu yuzden kaldirilmisti; combo
        // gozden kacmisti.
        //
        // OLCUM (`DailyChallengeReachabilityTest`, temkinli otopilot,
        // 180 sn, bes tohum): bes tohumun besinde `bestCombo = 0`. Yani
        // 1. KADEME BILE alinmiyordu ve o gun gelen oyuncu 500 coin'in
        // sifirini aliyordu. Ayni kosuda diger alti sablonun HEPSI 3/3
        // veriyor (tavanlar: skor ~14.7k, gecis 135, coin 103, boost 3410 m,
        // mesafe 7726 m, sure 180 sn).
        //
        // ⚠ "INSAN DA YAPAMAZ" DEMIYORUZ. Olcum otopilot verisi; risk alan
        // otopilot ayni bolumde bestCombo 10'a cikti ve 3/3 kademeyi ucte
        // uc tohumda aldi. Yani combo insan eliyle ulasilabilir. Sorun
        // ZORLUK degil, YAPI: gunluk gorev HEPSI-YA-HICBIRI ve tek metrik
        // uzerine kurulu, dolayisiyla bu sablon gunun tamamini TEK bir
        // istege bagli mekanige bagliyordu.
        //
        // DEGERLENDIRILEN VE REDDEDILEN SECENEKLER:
        //
        // 1) "Comboyu yalnizca 3. kademeye tasi" (kariyer kurali: beceri
        //    hedefi sadece ustalik yildizinda). UYGULANAMADI: gunluk
        //    kademelerin `targetValue`'lari ARTAN olmak zorunda
        //    (`DailyChallengeGeneratorTest.kademeler artan hedefli...`).
        //    Combo degerleri 2-5 arasinda, sayan hedefler ise 25/4500 gibi —
        //    [PassVehicles(25), ScoreAtLeast(4500), ComboAtLeast(3)] o
        //    degismezi kirar. Kademeler bilerek TEK METRIK: kart "biraz daha
        //    dayan" diye okunuyor, metrik degistiren bir kademe o okumayi
        //    bozar.
        // 2) "Kademeleri dusur". ISE YARAMAZ: en dusuk anlamli deger
        //    `ComboAtLeast(1)` = tek bir perfect dodge, yani 2026-08-17'de
        //    kaldirilan `dodge` hedefinin ta kendisi; temkinli oyun otuz
        //    kariyer bolumunde ve bu 180 sn'lik kosuda tek dodge yapmiyor.
        //
        // Mekanik hicbir yerde zayiflamadi: combo hala skoru x3'e kadar
        // katliyor, ON kariyer bolumunde ustalik yildizi
        // ([LevelCatalog] 6/7/11/14/17/20/23/25/26/29) ve haftalik gorevlerde
        // `bigCombos` sayaci olarak duruyor. Yalnizca gunun TEK KAPISI
        // olmaktan cikti.
        //
        // Yerine yeni sablon KONULMADI: gunluk sablonlar tek metrikli olmak
        // zorunda ve yukari sayan metriklerin geri kalani (dodge, combo)
        // beceri hedefi. Alti sablon kaliyor — `forDay` hash tabanli oldugu
        // icin sayinin 7 olmasinin bir onemi yok.
        challenge(
            id = "score",
            objectives = listOf(
                Objective.ScoreAtLeast(2000),
                Objective.ScoreAtLeast(4500),
                Objective.ScoreAtLeast(8000)
            )
        ),
        challenge(
            id = "survive",
            objectives = listOf(
                Objective.SurviveSeconds(45),
                Objective.SurviveSeconds(90),
                Objective.SurviveSeconds(140)
            )
        ),
        challenge(
            id = "coins",
            objectives = listOf(
                Objective.CoinsAtLeast(10),
                Objective.CoinsAtLeast(22),
                Objective.CoinsAtLeast(38)
            )
        ),
        challenge(
            id = "pass",
            objectives = listOf(
                Objective.PassVehicles(25),
                Objective.PassVehicles(55),
                Objective.PassVehicles(90)
            )
        )
    )

    // floorMod, `abs()` degil: abs(Int.MIN_VALUE) negatif kalir ve bazi
    // string'lerin hashCode'u tam olarak o degerdir — dizin disi hatasi verirdi.
    fun forDay(dayId: String): DailyChallenge =
        TEMPLATES[Math.floorMod(dayId.hashCode(), TEMPLATES.size)]

    /** "2026-08-13" formatinda gun kimligi (cihazin yerel saatine gore). */
    fun currentDayId(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(year, month, day)
    }
}
