package com.miniappfactory.krondrive.game

import com.miniappfactory.krondrive.data.AppLanguage

/** Bir kosunun hangi mod icin yapildigi. */
enum class RunMode { CAREER, ENDLESS, DAILY }

/** Kosunun nasil bitecegi. */
sealed class LevelGoal {
    /** Verilen sure boyunca hayatta kal (prototipteki 40 saniyelik kosu gibi). */
    data class SurviveTime(val seconds: Int) : LevelGoal()

    /** Verilen mesafeye ulas; sure limiti asilirsa bolum basarisiz olur. */
    data class ReachDistance(val meters: Int, val timeLimitSec: Int) : LevelGoal()

    val timeLimitSeconds: Int
        get() = when (this) {
            is SurviveTime -> seconds
            is ReachDistance -> timeLimitSec
        }
}

/**
 * Tek bir yildiz hedefi. Yeni hedef turu eklemek = buraya bir `data class` +
 * [isMet] dalinda bir satir; motor veya UI degismez.
 */
sealed class Objective {

    abstract fun isMet(stats: RunStats): Boolean

    /** Hedefin ilerleme durumu (0f..1f) — sonuc ekraninda cubuk olarak gosterilir. */
    open fun progress(stats: RunStats): Float = if (isMet(stats)) 1f else 0f

    abstract fun label(language: AppLanguage): String

    /**
     * Hedef "yukari sayan" mi (dodge/mesafe/puan gibi)? Gunluk gorevde kosu,
     * hedef tutturuldugu ANDA basariyla biter — ama bu sadece yukari sayan
     * hedefler icin anlamli. `CompleteRun` veya `BrakeTapsAtMost` gibi hedefler
     * kosunun basinda zaten "saglanmis" gorunur, o yuzden false donerler.
     */
    open val autoCompletes: Boolean get() = true

    /**
     * Oyun ici HUD icin kisa etiket + "3/8" bicimi ilerleme. HUD'da tam cumle
     * gostermek ekranin ustunu kapatiyordu (oyuncu geri bildirimi, 2026-08-13).
     */
    open fun shortLabel(language: AppLanguage): String = label(language)

    open fun currentValue(stats: RunStats): Int? = null

    open val targetValue: Int? get() = null

    fun progressText(stats: RunStats): String? {
        val current = currentValue(stats) ?: return null
        val target = targetValue ?: return null
        return "${minOf(current, target)}/$target"
    }

    /** Bolumu tamamla (kaza yapmadan hedefe ulas). */
    data object CompleteRun : Objective() {
        override fun isMet(stats: RunStats) = stats.completed
        override val autoCompletes: Boolean get() = false
        override fun label(language: AppLanguage) =
            language.pick(tr = "Bölümü tamamla", en = "Complete the level")

        override fun shortLabel(language: AppLanguage) =
            language.pick(tr = "BİTİR", en = "FINISH")
    }

    data class PassVehicles(val count: Int) : Objective() {
        override fun isMet(stats: RunStats) = stats.vehiclesPassed >= count
        override fun progress(stats: RunStats) = ratio(stats.vehiclesPassed, count)
        override fun label(language: AppLanguage) =
            language.pick(tr = "$count araç geç", en = "Pass $count vehicles")
        override fun shortLabel(language: AppLanguage) =
            language.pick(tr = "GEÇİŞ", en = "PASS")
        override fun currentValue(stats: RunStats) = stats.vehiclesPassed
        override val targetValue: Int get() = count
    }

    data class PerfectDodges(val count: Int) : Objective() {
        override fun isMet(stats: RunStats) = stats.perfectDodges >= count
        override fun progress(stats: RunStats) = ratio(stats.perfectDodges, count)
        override fun label(language: AppLanguage) =
            language.pick(tr = "$count Perfect Dodge yap", en = "Perform $count Perfect Dodges")
        override fun shortLabel(language: AppLanguage) = "DODGE"
        override fun currentValue(stats: RunStats) = stats.perfectDodges
        override val targetValue: Int get() = count
    }

    data class BoostDistance(val meters: Int) : Objective() {
        override fun isMet(stats: RunStats) = stats.boostDistanceMeters >= meters
        override fun progress(stats: RunStats) = ratio(stats.boostDistanceMeters, meters)
        override fun label(language: AppLanguage) =
            language.pick(
                tr = "Boost ile $meters m ilerle",
                en = "Travel $meters m using Boost"
            )
        override fun shortLabel(language: AppLanguage) = "BOOST"
        override fun currentValue(stats: RunStats) = stats.boostDistanceMeters
        override val targetValue: Int get() = meters
    }

    data class BrakeTapsAtMost(val count: Int) : Objective() {
        override fun isMet(stats: RunStats) = stats.brakeTaps <= count
        override fun progress(stats: RunStats) = if (isMet(stats)) 1f else 0f
        override val autoCompletes: Boolean get() = false
        override fun label(language: AppLanguage) =
            language.pick(
                tr = "Frene en fazla $count kez bas",
                en = "Use brake at most $count times"
            )
        override fun shortLabel(language: AppLanguage) =
            language.pick(tr = "FREN", en = "BRAKE")
        override fun currentValue(stats: RunStats) = stats.brakeTaps
        override val targetValue: Int get() = count
    }

    data class ScoreAtLeast(val points: Int) : Objective() {
        override fun isMet(stats: RunStats) = stats.score >= points
        override fun progress(stats: RunStats) = ratio(stats.score, points)
        override fun label(language: AppLanguage) =
            language.pick(tr = "$points puan yap", en = "Score $points points")
        override fun shortLabel(language: AppLanguage) =
            language.pick(tr = "PUAN", en = "SCORE")
        override fun currentValue(stats: RunStats) = stats.score
        override val targetValue: Int get() = points
    }

    data class ComboAtLeast(val combo: Int) : Objective() {
        override fun isMet(stats: RunStats) = stats.bestCombo >= combo
        override fun progress(stats: RunStats) = ratio(stats.bestCombo, combo)
        override fun label(language: AppLanguage) =
            language.pick(tr = "${combo}x combo yap", en = "Reach a ${combo}x combo")
        override fun shortLabel(language: AppLanguage) = "COMBO"
        override fun currentValue(stats: RunStats) = stats.bestCombo
        override val targetValue: Int get() = combo
    }

    data class CoinsAtLeast(val coins: Int) : Objective() {
        override fun isMet(stats: RunStats) = stats.coinsCollected >= coins
        override fun progress(stats: RunStats) = ratio(stats.coinsCollected, coins)
        override fun label(language: AppLanguage) =
            language.pick(tr = "$coins coin topla", en = "Collect $coins coins")
        override fun shortLabel(language: AppLanguage) = "COIN"
        override fun currentValue(stats: RunStats) = stats.coinsCollected
        override val targetValue: Int get() = coins
    }

    data class FinishUnderSeconds(val seconds: Int) : Objective() {
        override fun isMet(stats: RunStats) =
            stats.completed && stats.timeSurvivedSec <= seconds
        override val autoCompletes: Boolean get() = false
        override fun label(language: AppLanguage) =
            language.pick(
                tr = "$seconds saniyenin altında bitir",
                en = "Finish in under $seconds seconds"
            )
        override fun shortLabel(language: AppLanguage) =
            language.pick(tr = "SÜRE", en = "TIME")
    }

    /**
     * Verilen sure kadar hayatta kal. [FinishUnderSeconds] ile karistirma:
     * o "bitirme suresi" hedefi, bu YUKARI sayan bir hedef — gunluk gorevin
     * sure kademelerinde (60/90/130 sn) kullaniliyor.
     */
    data class SurviveSeconds(val seconds: Int) : Objective() {
        override fun isMet(stats: RunStats) = stats.timeSurvivedSec >= seconds
        override fun progress(stats: RunStats) = ratio(stats.timeSurvivedSec, seconds)
        override fun label(language: AppLanguage) =
            language.pick(tr = "$seconds saniye hayatta kal", en = "Survive $seconds seconds")
        override fun shortLabel(language: AppLanguage) =
            language.pick(tr = "SÜRE", en = "TIME")
        override fun currentValue(stats: RunStats) = stats.timeSurvivedSec
        override val targetValue: Int get() = seconds
    }

    data class DistanceAtLeast(val meters: Int) : Objective() {
        override fun isMet(stats: RunStats) = stats.distanceMeters >= meters
        override fun progress(stats: RunStats) = ratio(stats.distanceMeters, meters)
        override fun label(language: AppLanguage) =
            language.pick(tr = "$meters m ilerle", en = "Travel $meters m")
        override fun shortLabel(language: AppLanguage) =
            language.pick(tr = "MESAFE", en = "DIST")
        override fun currentValue(stats: RunStats) = stats.distanceMeters
        override val targetValue: Int get() = meters
    }

    protected fun ratio(current: Int, target: Int): Float =
        if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
}

/**
 * Bir bolumun tanimi. Seviye eklemek icin [LevelCatalog] icine bir satir
 * yazmak yeterli — motor, UI ve kayit katmani degismez.
 */
data class LevelDef(
    val id: Int,
    val goal: LevelGoal,
    /** Uc yildiz hedefi; [0] birinci yildiz (bolumun kendisi) demektir. */
    val stars: List<Objective>,
    /**
     * Gunluk gorev de bir [LevelDef] olarak calisir ama yildiz vermez —
     * odulu sabit coin'dir (bkz. [com.miniappfactory.krondrive.data.DailyChallenge]).
     */
    val awardsStars: Boolean = true,
    /**
     * Bolumun baslangic hizi (km/h). null ise [GameConfig.BASE_SPEED] (80 km/h)
     * kullanilir. Ilk bolumler daha yavas basliyor: 80 km/h ile acilis, oyunu
     * ilk kez acan biri icin fazla hizli (oyuncu geri bildirimi, 2026-08-13).
     * Skordan gelen hizlanma yine ayni, yani bolum yine hizlanarak ilerliyor.
     */
    val startSpeedKmh: Int? = null,
    /**
     * Bu bolumde SKORDAN gelen hizlanmanin carpani. 1.0 = tam rampa (varsayilan,
     * 8. bolumden sonrasi). Kucuk deger = bolum daha yavas hizlanir.
     *
     * ⚠ HANGI TERIME UYGULANDIGI ONEMLI — yanlis secim fikri tersine cevirir.
     * Formul su ([GameEngine.updateSpeed]):
     *
     *     hedef = tabanHiz + speedRampScale * min(hizTavani, skor / 600)
     *
     * Yani carpan `min(...)`in SONUCUNA uygulaniyor. Bunun anlami: tavan
     * SABITLENMIYOR, ORANTILI KUCULUYOR — SPEED yukseltmesinin degerinin
     * %65'i korunuyor. Alternatif (yalnizca rampayi yavaslatmak, tavani
     * birakmak) OLCULDU ve REDDEDILDI: tavan degismedigi icin kosu sonunda
     * tepki butcesi hic iyilesmiyor ve tam yukseltmeli oyuncuda hicbir sey
     * degismiyordu (docs/REVIEW_GAMEPLAY.md).
     *
     * NEDEN VAR: sahibi (2026-08-16) *"4. levelde 30. saniyede max hiza
     * standart araba ile ulasmak dogru degil"* dedi. Olcum onu dogruladi ve
     * daha kotusunu buldu: bolum 4 tavana ~23 saniyede ulasiyor ve suresinin
     * %42'sini tavanda geciriyor.
     *
     * NEDEN GLOBAL IVME DEGIL: sahibinin ilk sezgisi `ACCEL_RATE_BASE`'i
     * dusurmekti. Olcum bunun KENDI fikrini bozdugunu gosterdi — bir boost
     * darbesinden alinan etki %94'ten %81'e iner, yani "boost hizin ana
     * araci olsun" istegi zayiflar. Bolum bazli carpan boost ekonomisine
     * hic dokunmuyor.
     */
    val speedRampScale: Float = 1f,
    /**
     * Bu bolumdeki trafik yogunlugu carpani. 1.0 = tam yogunluk
     * ([GameConfig.OBSTACLE_SPAWN_INTERVAL_SEC]); 0.5 = birim zamanda yarisi
     * kadar arac. Dogma araligi bu carpana BOLUNUR.
     *
     * Neden var: ilk bolumler "hala fazla zor" geri bildirimi aldi (sahibi,
     * 2026-08-14 — ikinci kez). Baslangic hizini dusurmek yetmedi, cunku
     * zorlugun asil ekseni saniyedeki arac sayisi. Yogunluk en OKUNABILIR
     * zorluk ekseni: oyuncu neyin degistigini gorur (bkz. `game-scenario`
     * skill'i, "zorlugu artirmanin dogru sirasi").
     *
     * Varsayilan 1.0 — mevcut bolumlerin hicbiri etkilenmez.
     */
    val trafficDensity: Float = 1f,
    /**
     * Bu bolumde CIFT DOGUS olasiligi: bir dogusta iki seridin ayni anda
     * kapanma sansi. null = bolum numarasindan turet
     * ([GameConfig.doubleSpawnChance]) — normal hal budur.
     *
     * Acik deger yalnizca TEK bir bolumu rampadan ayirmak gerektiginde
     * yazilir (orn. bir nefes bolumu). Rampanin kendisi burada degil
     * [GameConfig]'te ayarlanir.
     */
    val doubleSpawnChance: Float? = null
) {
    /** Motorun kullandigi olasilik: acik deger varsa o, yoksa rampadan. */
    val effectiveDoubleSpawnChance: Float
        get() = doubleSpawnChance ?: GameConfig.doubleSpawnChance(id)

    init {
        require(stars.isNotEmpty()) { "Level $id: en az bir hedef olmali" }
        require(doubleSpawnChance == null || doubleSpawnChance in 0f..1f) {
            "Level $id: doubleSpawnChance 0..1 araliginda olmali"
        }
        require(!awardsStars || stars.size == 3) {
            "Level $id: yildiz veren bolumlerde tam olarak 3 hedef olmali"
        }
        require(trafficDensity > 0f) {
            "Level $id: trafficDensity pozitif olmali (0 = hic arac dogmaz)"
        }
    }
}

/** Bir kosu boyunca toplanan olcumler. Yildiz degerlendirmesinin tek girdisi. */
data class RunStats(
    val score: Int = 0,
    val timeSurvivedSec: Int = 0,
    val distanceMeters: Int = 0,
    val boostDistanceMeters: Int = 0,
    val vehiclesPassed: Int = 0,
    val perfectDodges: Int = 0,
    val bestCombo: Int = 0,
    /** Kac kez 5x veya ustu combo yapildi (haftalik gorev sayaci). */
    val bigCombos: Int = 0,
    val coinsCollected: Int = 0,
    val brakeTaps: Int = 0,
    val crashed: Boolean = false,
    val revivesUsed: Int = 0,
    /** Bolum hedefine ulasildi mi (sonsuz modda her zaman false). */
    val completed: Boolean = false
)

/** Kosu bittiginde UI'a ve kayit katmanina giden ozet. */
data class RunResult(
    val mode: RunMode,
    val levelId: Int?,
    val stats: RunStats,
    val stars: Int,
    /**
     * Bu kosuda ILK KEZ tamamlanan gorev sayisi. Coin odulu buna gore verilir
     * — daha once alinmis gorev ikinci kez odenmez. Sonuc ekrani da bunu
     * gosterir, yoksa "gorevleri yaptim ama coin gelmedi" gibi okunurdu.
     */
    val newStars: Int = 0,
    /**
     * Bolum GECILDI mi: kariyerde bolumun UC GOREVININ DE tamamlanmis olmasi.
     *
     * Sahibi karari (2026-08-15): *"gorevleri tamamlamadiysa neden geciyor ki
     * bolumu, gorevleri yapmak bolumun gecmenin ilk sarti"*. Eskiden sureyi
     * doldurmak yetiyordu ve tek bir gorevle bir sonraki bolum aciliyordu.
     *
     * `stats.completed` bundan FARKLIDIR: o, bolumun kendi hedefinin (sureyi
     * doldur / mesafeye ulas) saglandigini soyler ve `Objective.CompleteRun`
     * onu olcer. Ikisi ayri tutulmali, aksi halde "bolumu tamamla" gorevi
     * kendi kendini olcerdi.
     */
    val passed: Boolean = false,
    val coinsEarned: Int,
    val xpEarned: Int,
    /**
     * Gunluk gorevde bu kosuda ulasilan kademe sayisi (0..3). Kariyer ve
     * sonsuz modda her zaman 0 — gunluk gorev yildiz vermez, kademe verir
     * (bkz. [com.miniappfactory.krondrive.data.DailyChallenge]).
     */
    val dailyTiers: Int = 0,
    /** Sonsuz modda yeni rekor kirildi mi. */
    val newRecord: Boolean = false,
    /** Sonsuz modda rekora kac saniye kaldi (yakinsa gosterilir, degilse null). */
    val secondsFromRecord: Int? = null
)

/** Motorun her karede uretebildigi olaylar (ses, HUD animasyonu, reklam akisi). */
sealed class GameEvent {
    data class PerfectDodge(val combo: Int, val multiplier: Float, val bonusScore: Int) : GameEvent()
    data object ComboBroken : GameEvent()
    data class CoinPicked(val total: Int) : GameEvent()
    data object VehiclePassed : GameEvent()
    data object BoostStarted : GameEvent()
    /** Carpisma ani — [saved] true ise Second Chance booster'i devreye girdi. */
    data class Crash(val saved: Boolean) : GameEvent()
    data class Finished(val result: RunResult) : GameEvent()
}

/** Kosunun akis durumu. */
enum class RunPhase { COUNTDOWN, RUNNING, PAUSED, CRASHED, FINISHED }

/** Arka plan temasi (prototipteki THEMES dizisi). */
enum class RoadTheme { GRASS, BEACH, CROWD, NIGHT }
