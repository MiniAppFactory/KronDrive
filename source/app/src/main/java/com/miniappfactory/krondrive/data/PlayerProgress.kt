package com.miniappfactory.krondrive.data

import com.miniappfactory.krondrive.game.CarCatalog
import com.miniappfactory.krondrive.game.CarStyle
import com.miniappfactory.krondrive.game.GameConfig
import com.miniappfactory.krondrive.game.UpgradeLevels

/** Coin ile satin alinan, kosu basinda etkinlestirilen guclendiriciler. */
enum class BoosterType(val coinPrice: Int) {
    /** Kosunun ilk saniyelerinde boost enerjisi harcanmaz. */
    TURBO_START(150),

    /** Ilk carpismayi yok sayar. */
    SECOND_CHANCE(400),

    /** Kosudan kazanilan coin iki katina cikar. */
    DOUBLE_REWARD(300),

    /** Kosu boyunca skor +%25. */
    SCORE_BOOSTER(250);

    fun title(language: AppLanguage): String = when (this) {
        TURBO_START -> language.pick(tr = "Turbo Başlangıç", en = "Turbo Start")
        SECOND_CHANCE -> language.pick(tr = "İkinci Şans", en = "Second Chance")
        DOUBLE_REWARD -> language.pick(tr = "Çift Ödül", en = "Double Reward")
        SCORE_BOOSTER -> language.pick(tr = "Skor Yükseltici", en = "Score Booster")
    }

    fun description(language: AppLanguage): String = when (this) {
        TURBO_START -> language.pick(
            tr = "İlk ${GameConfig.TURBO_START_FREE_BOOST_SEC.toInt()} saniye boost bedava",
            en = "Free boost for the first ${GameConfig.TURBO_START_FREE_BOOST_SEC.toInt()} seconds"
        )

        SECOND_CHANCE -> language.pick(
            tr = "İlk çarpışmayı yok sayar",
            en = "Ignores your first crash"
        )

        DOUBLE_REWARD -> language.pick(
            tr = "Bu koşunun coin ödülü 2 katı",
            en = "Doubles this run's coin reward"
        )

        SCORE_BOOSTER -> language.pick(
            tr = "Bu koşu boyunca skor +%25",
            en = "+25% score for this run"
        )
    }
}

/** Cihazda saklanan tum oyuncu ilerlemesi. */
data class PlayerProgress(
    val coins: Int = STARTING_COINS,
    val xp: Int = 0,
    val highestUnlockedLevel: Int = 1,
    /** bolumId -> kazanilan en iyi yildiz sayisi (0..3) */
    val levelStars: Map<Int, Int> = emptyMap(),
    val upgrades: UpgradeLevels = UpgradeLevels(),
    val ownedBoosters: Map<BoosterType, Int> = emptyMap(),
    val endlessBestSeconds: Int = 0,
    val endlessBestScore: Int = 0,
    val soundEnabled: Boolean = true,
    /**
     * Titresim (haptik) tercihi — 2026-08-17'de eklendi.
     *
     * VARSAYILAN ACIK olmak ZORUNDA: bu alan gelmeden once serit degistirme,
     * korna ve carpisma titresimleri KOSULSUZ calisiyordu. Kaydinda bu anahtar
     * bulunmayan mevcut oyuncu icin `true` okunur (bkz. GameStateRepository)
     * ve oyun bugunku gibi davranmaya devam eder; goc gerektiren tek nokta bu.
     */
    val vibrationEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.EN,
    /**
     * Oyuncu dili KENDISI secti mi. false ise ilk acilis dil ekrani gosterilir;
     * [language] o ana kadar cihazin dilinden tahmin edilmis demektir.
     */
    val languageChosen: Boolean = false,
    val hasSeenOnboarding: Boolean = false,
    /** Son gecis reklamindan bu yana tamamlanan bolum sayisi. */
    val levelsSinceInterstitial: Int = 0,
    /** Son gecis reklamindan bu yana bitirilen sonsuz mod kosusu sayisi. */
    val endlessRunsSinceInterstitial: Int = 0,
    /** Bugun odullu reklamla kac kez coin alindi (gunluk sinir icin). */
    val rewardedCoinsToday: Int = 0,

    // --- Arac ozellestirme (bkz. game/CarCatalog.kt) ---
    //
    // Secili id'ler repository okurken DOGRULANIR: bilinmeyen ya da sahip
    // olunmayan bir id varsayilana duser, yani eski kayitlar bozulmaz.
    val carShapeId: String = CarCatalog.DEFAULT_SHAPE_ID,
    val carColorId: String = CarCatalog.DEFAULT_COLOR_ID,
    /** Satin alinmis govde sekilleri (bedava olanlar listede olmaz). */
    val ownedCarShapes: Set<String> = emptySet(),
    val ownedCarColors: Set<String> = emptySet()
) {
    val totalStars: Int get() = levelStars.values.sum()

    /** Cizim ve garaj onizlemesi icin cozulmus gorunum. */
    val carStyle: CarStyle get() = CarCatalog.style(carShapeId, carColorId)

    /** XP'den turetilen arac seviyesi (garajda gosterilir). */
    val carLevel: Int get() = carLevelForXp(xp)

    /** Mevcut seviyede bir sonraki arac seviyesine ilerleme (0f..1f). */
    val carLevelProgress: Float
        get() = (xp % GameConfig.XP_PER_CAR_LEVEL).toFloat() / GameConfig.XP_PER_CAR_LEVEL

    fun starsOf(levelId: Int): Int = levelStars[levelId] ?: 0

    fun boosterCount(type: BoosterType): Int = ownedBoosters[type] ?: 0

    /** Bugun odullu reklamla daha kac kez coin alinabilir. */
    val rewardedCoinsRemainingToday: Int
        get() = (GameConfig.REWARDED_COIN_DAILY_LIMIT - rewardedCoinsToday).coerceAtLeast(0)

    companion object {
        /**
         * Ilk kurulumda verilen baslangic coini.
         *
         * ⚠⚠ GECICI TEST DEGERI — YAYINDAN ONCE 100'E GERI CEKILECEK. ⚠⚠
         *
         * Sahibi butun araclari (en pahalisi 5000 coin) cihazda denemek
         * istedi ve 2026-08-17'de gecici olarak 100000 verildi. Sahibinin
         * kendi sozu: *"aab yaparken degistiririz"*.
         *
         * BU DEGERLE YAYINA CIKILIRSA oyunun butun ekonomisi anlamsizlasir:
         * her arac, her boya ve butun yukseltmeler (toplam ~51.000) ilk
         * saniyede alinabilir hale gelir.
         *
         * Bir test bu degeri KILITLIYOR (PlayerProgressCarTest) — release
         * hazirlanirken o test kirilarak hatirlatma yapar.
         */
        const val STARTING_COINS = 100_000

        /** Yayina cikacak gercek deger; [STARTING_COINS] buna geri donmeli. */
        const val STARTING_COINS_RELEASE = 100

        /**
         * XP -> arac seviyesi. Repository de bunu kullanir (seviye sarti olan
         * arac boyalari icin); formul TEK YERDE kalsin diye disari acildi.
         */
        fun carLevelForXp(xp: Int): Int = 1 + xp / GameConfig.XP_PER_CAR_LEVEL
    }
}

/** Haftalik gorev turleri. */
enum class MissionType { COMPLETE_LEVELS, DRIVE_DISTANCE, PASS_VEHICLES, PERFECT_DODGES, BIG_COMBOS }

/** Bir gorevin kademesi: hedefe ulasinca odul talep edilebilir. */
data class MissionTier(val target: Int, val rewardCoins: Int)

data class WeeklyMissionDef(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val tiers: List<MissionTier>,
    val type: MissionType
) {
    /** Baslikta {count}, o an aktif olan kademenin hedefiyle degistirilir. */
    fun title(language: AppLanguage, count: Int): String =
        language.pick(tr = titleTr, en = titleEn).replace("{count}", "$count")
}

data class WeeklyMissionProgress(
    val weekId: String,
    val missions: List<WeeklyMissionDef>,
    /** gorevId -> kumulatif ham sayac */
    val progress: Map<String, Int> = emptyMap(),
    /** "gorevId#kademeIndeksi" formatinda talep edilmis oduller */
    val claimed: Set<String> = emptySet()
) {
    fun progressOf(mission: WeeklyMissionDef): Int = progress[mission.id] ?: 0

    fun isClaimed(mission: WeeklyMissionDef, tierIndex: Int): Boolean =
        "${mission.id}#$tierIndex" in claimed

    /** Odul talep edilebilecek (hedefe ulasilmis ama alinmamis) kademe var mi. */
    fun hasClaimable(): Boolean = missions.any { mission ->
        mission.tiers.withIndex().any { (index, tier) ->
            progressOf(mission) >= tier.target && !isClaimed(mission, index)
        }
    }

    /** Haftalik sandik: tum gorevlerin tum kademeleri tamamlandiysa acilir. */
    fun allTiersComplete(): Boolean = missions.all { mission ->
        progressOf(mission) >= mission.tiers.last().target
    }
}

/** Gunun gorevi ve kayitli durumu. */
data class DailyChallengeState(
    val dayId: String,
    val challenge: DailyChallenge,
    /** Bugun odulu ALINMIS kademe sayisi (0..3). Gun degisince sifirlanir. */
    val tiersGranted: Int = 0
) {
    /** Uc kademenin ucu de alindiysa gunluk gorev bitmistir. */
    val completed: Boolean get() = tiersGranted >= challenge.tiers.size

    /** Bir sonraki kademe (hepsi alindiysa null). */
    val nextTier: DailyTier? get() = challenge.tiers.getOrNull(tiersGranted)

    /** Bugun geriye kalan coin. */
    val remainingCoins: Int
        get() = challenge.rewardBetween(tiersGranted, challenge.tiers.size)
}
