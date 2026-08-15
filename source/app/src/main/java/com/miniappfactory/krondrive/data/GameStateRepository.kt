package com.miniappfactory.krondrive.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miniappfactory.krondrive.game.CarCatalog
import com.miniappfactory.krondrive.game.CarItem
import com.miniappfactory.krondrive.game.GameConfig
import com.miniappfactory.krondrive.game.UpgradeCatalog
import com.miniappfactory.krondrive.game.UpgradeLevels
import com.miniappfactory.krondrive.game.UpgradeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.gameDataStore by preferencesDataStore(name = "kron_drive_progress")

/**
 * Tum kalici oyuncu verisi. Boom Blocks ile ayni desen: tek bir DataStore
 * Preferences dosyasi, Map/Set alanlari `anahtar:deger,anahtar:deger` string'i
 * olarak kodlanir (Room/kotlinx.serialization bagimliligi eklememek icin).
 *
 * Coin harcamasi gibi "oku-degistir-yaz" islemleri TEK bir `edit {}` blogunda
 * yapilir — DataStore bu blogu atomik calistirir, iki hizli tiklama coini iki
 * kez harcayamaz.
 */
class GameStateRepository(private val context: Context) {

    private object Keys {
        val COINS = intPreferencesKey("coins")
        val XP = intPreferencesKey("xp")
        val HIGHEST_LEVEL = intPreferencesKey("highest_unlocked_level")
        val LEVEL_STARS = stringPreferencesKey("level_stars")

        val UPGRADE_SPEED = intPreferencesKey("upgrade_speed")
        val UPGRADE_ACCELERATION = intPreferencesKey("upgrade_acceleration")
        val UPGRADE_BRAKE = intPreferencesKey("upgrade_brake")
        val UPGRADE_BOOST = intPreferencesKey("upgrade_boost")

        val OWNED_BOOSTERS = stringPreferencesKey("owned_boosters")

        // Arac ozellestirme. Bu anahtarlar v1.0.2'de eklendi; eski kurulumda
        // yoklar ve okuma varsayilan arac + bos envanter dondurur.
        val CAR_SHAPE = stringPreferencesKey("car_shape")
        val CAR_COLOR = stringPreferencesKey("car_color")
        val OWNED_CAR_SHAPES = stringPreferencesKey("owned_car_shapes")
        val OWNED_CAR_COLORS = stringPreferencesKey("owned_car_colors")

        val ENDLESS_BEST_SECONDS = intPreferencesKey("endless_best_seconds")
        val ENDLESS_BEST_SCORE = intPreferencesKey("endless_best_score")

        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val LANGUAGE = stringPreferencesKey("language")

        /**
         * Oyuncu dili kendisi secti mi. Yalnizca [LANGUAGE] anahtarina bakmak
         * yetmez: eski surumler dili hic secmeden de yazabiliyordu ve o
         * kayitlar icin dil ekranini bir kez gostermek dogru davranis.
         */
        val LANGUAGE_CHOSEN = booleanPreferencesKey("language_chosen")

        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")

        val LEVELS_SINCE_INTERSTITIAL = intPreferencesKey("levels_since_interstitial")
        val ENDLESS_RUNS_SINCE_INTERSTITIAL = intPreferencesKey("endless_runs_since_interstitial")

        val MISSION_WEEK_ID = stringPreferencesKey("mission_week_id")
        val MISSION_PROGRESS = stringPreferencesKey("mission_progress")
        val MISSION_CLAIMED = stringPreferencesKey("mission_claimed")

        val DAILY_DAY_ID = stringPreferencesKey("daily_day_id")

        /** Bugun odulu odenmis kademe sayisi (0..3). */
        val DAILY_TIER = intPreferencesKey("daily_tier")

        /**
         * v1.0.1'den kalan "gun tamamlandi" bayragi. Yeni surumde yerini
         * [DAILY_TIER] aldi; eski kurulumdan gelen kayitta true ise o gun
         * tum kademeler odenmis sayilir (odul ikinci kez verilmesin diye).
         */
        val DAILY_COMPLETED = booleanPreferencesKey("daily_completed")

        // Odullu "coin kazan" reklaminin gunluk sayaci.
        val REWARD_COIN_DAY = stringPreferencesKey("reward_coin_day")
        val REWARD_COIN_COUNT = intPreferencesKey("reward_coin_count")
    }

    val playerProgress: Flow<PlayerProgress> = context.gameDataStore.data.map { prefs ->
        val ownedShapes = decodeStringSet(prefs[Keys.OWNED_CAR_SHAPES])
        val ownedColors = decodeStringSet(prefs[Keys.OWNED_CAR_COLORS])
        PlayerProgress(
            coins = prefs[Keys.COINS] ?: PlayerProgress.STARTING_COINS,
            xp = prefs[Keys.XP] ?: 0,
            highestUnlockedLevel = prefs[Keys.HIGHEST_LEVEL] ?: 1,
            levelStars = decodeIntMap(prefs[Keys.LEVEL_STARS]),
            upgrades = UpgradeLevels(
                speed = prefs[Keys.UPGRADE_SPEED] ?: 1,
                acceleration = prefs[Keys.UPGRADE_ACCELERATION] ?: 1,
                brake = prefs[Keys.UPGRADE_BRAKE] ?: 1,
                boost = prefs[Keys.UPGRADE_BOOST] ?: 1
            ),
            ownedBoosters = decodeBoosterMap(prefs[Keys.OWNED_BOOSTERS]),
            endlessBestSeconds = prefs[Keys.ENDLESS_BEST_SECONDS] ?: 0,
            endlessBestScore = prefs[Keys.ENDLESS_BEST_SCORE] ?: 0,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            // Ilk kurulumda cihazin diline gore secilir, sonra kullanicinin
            // ayarlardaki tercihi kalicidir.
            language = prefs[Keys.LANGUAGE]?.let { AppLanguage.fromCode(it) }
                ?: AppLanguage.fromSystemLocale(),
            languageChosen = prefs[Keys.LANGUAGE_CHOSEN] ?: false,
            hasSeenOnboarding = prefs[Keys.HAS_SEEN_ONBOARDING] ?: false,
            levelsSinceInterstitial = prefs[Keys.LEVELS_SINCE_INTERSTITIAL] ?: 0,
            endlessRunsSinceInterstitial = prefs[Keys.ENDLESS_RUNS_SINCE_INTERSTITIAL] ?: 0,
            // Gun ILERI gittiyse sayac okurken de sifir gorunur; saat geri
            // alinmissa korunur (bkz. isFreshDay).
            rewardedCoinsToday = if (
                isFreshDay(prefs[Keys.REWARD_COIN_DAY], DailyChallengeGenerator.currentDayId())
            ) {
                0
            } else {
                prefs[Keys.REWARD_COIN_COUNT] ?: 0
            },
            // Secim OKURKEN dogrulanir: bilinmeyen bir id ya da sahip
            // olunmayan bir icerik sessizce varsayilana duser.
            carShapeId = CarCatalog.selectedShape(prefs[Keys.CAR_SHAPE], ownedShapes).id,
            carColorId = CarCatalog.selectedColor(prefs[Keys.CAR_COLOR], ownedColors).id,
            ownedCarShapes = ownedShapes,
            ownedCarColors = ownedColors
        )
    }

    val weeklyMissionProgress: Flow<WeeklyMissionProgress> = context.gameDataStore.data.map { prefs ->
        val currentWeek = currentWeekId()
        val storedWeek = prefs[Keys.MISSION_WEEK_ID]
        // Hafta degistiyse okurken de sifir gosterilir; kalici sifirlama ilk
        // yazma isleminde (incrementMissionProgress) yapilir.
        val sameWeek = storedWeek == currentWeek
        WeeklyMissionProgress(
            weekId = currentWeek,
            missions = WeeklyMissionGenerator.forWeek(currentWeek),
            progress = if (sameWeek) decodeStringIntMap(prefs[Keys.MISSION_PROGRESS]) else emptyMap(),
            claimed = if (sameWeek) decodeStringSet(prefs[Keys.MISSION_CLAIMED]) else emptySet()
        )
    }

    val dailyChallengeState: Flow<DailyChallengeState> = context.gameDataStore.data.map { prefs ->
        val today = DailyChallengeGenerator.currentDayId()
        val challenge = DailyChallengeGenerator.forDay(today)
        DailyChallengeState(
            dayId = today,
            challenge = challenge,
            tiersGranted = grantedTiers(prefs, today, challenge.tiers.size)
        )
    }

    /** Gun degistiyse 0; eski surumun "tamamlandi" bayragi varsa tum kademeler. */
    private fun grantedTiers(
        prefs: androidx.datastore.preferences.core.Preferences,
        today: String,
        tierCount: Int
    ): Int {
        if (!isFreshDay(prefs[Keys.DAILY_DAY_ID], today)) {
            val stored = prefs[Keys.DAILY_TIER]
            if (stored != null) return stored.coerceIn(0, tierCount)
            return if (prefs[Keys.DAILY_COMPLETED] == true) tierCount else 0
        }
        return 0
    }

    private fun isFreshDay(storedDay: String?, today: String): Boolean =
        Companion.isFreshDay(storedDay, today)

    // ---------------------------------------------------------------
    // Coin / XP
    // ---------------------------------------------------------------

    suspend fun addCoins(amount: Int) {
        if (amount <= 0) return
        context.gameDataStore.edit { prefs ->
            prefs[Keys.COINS] = (prefs[Keys.COINS] ?: PlayerProgress.STARTING_COINS) + amount
        }
    }

    suspend fun addXp(amount: Int) {
        if (amount <= 0) return
        context.gameDataStore.edit { prefs ->
            prefs[Keys.XP] = (prefs[Keys.XP] ?: 0) + amount
        }
    }

    /** Yeterli coin varsa harcar ve true doner. */
    suspend fun spendCoins(amount: Int): Boolean {
        var success = false
        context.gameDataStore.edit { prefs ->
            val current = prefs[Keys.COINS] ?: PlayerProgress.STARTING_COINS
            if (current >= amount) {
                prefs[Keys.COINS] = current - amount
                success = true
            }
        }
        return success
    }

    /**
     * Odullu reklam karsiligi coin. Gunluk sinir ve coin ekleme TEK atomik
     * islemde — sinir asilirsa hicbir sey degismez ve false doner.
     *
     * [amount] verilmezse standart odul ([GameConfig.REWARDED_COIN_AMOUNT]).
     * Sonuc ekranindaki "odulu ikiye katla" reklami da BURADAN gecer, yani
     * gunluk sinir iki reklam yuzeyi arasinda PAYLASILIR.
     */
    suspend fun grantRewardedCoins(amount: Int = GameConfig.REWARDED_COIN_AMOUNT): Boolean {
        if (amount <= 0) return false
        var success = false
        context.gameDataStore.edit { prefs ->
            val today = DailyChallengeGenerator.currentDayId()
            val freshDay = isFreshDay(prefs[Keys.REWARD_COIN_DAY], today)
            val used = if (freshDay) 0 else prefs[Keys.REWARD_COIN_COUNT] ?: 0
            if (used < GameConfig.REWARDED_COIN_DAILY_LIMIT) {
                // Saat geri alinmissa kayitli (ileri) gun korunur; sayac ancak
                // gercekten yeni bir gunde sifirlanir (bkz. isFreshDay).
                if (freshDay) prefs[Keys.REWARD_COIN_DAY] = today
                prefs[Keys.REWARD_COIN_COUNT] = used + 1
                prefs[Keys.COINS] = (prefs[Keys.COINS] ?: PlayerProgress.STARTING_COINS) + amount
                success = true
            }
        }
        return success
    }

    // ---------------------------------------------------------------
    // Bolum ilerlemesi
    // ---------------------------------------------------------------

    /** Bolum sonucu: en iyi yildiz sayisi saklanir, sonraki bolum acilir. */
    /**
     * Bolum sonucu. [passed] yalnizca TUM gorevler tamamlandiysa true gelir ve
     * bir sonraki bolumun kilidi ancak o zaman acilir.
     *
     * Eskiden tek bir gorev (`stars > 0`) yetiyordu; sahibi karari
     * (2026-08-15): gorevleri yapmak bolumu gecmenin ilk sarti.
     * Tamamlanan gorev sayisi yine kaydediliyor — haritadaki ilerleme
     * isaretleri ve gorev odulu ondan besleniyor.
     */
    suspend fun recordLevelResult(levelId: Int, stars: Int, passed: Boolean) {
        context.gameDataStore.edit { prefs ->
            val starsMap = decodeIntMap(prefs[Keys.LEVEL_STARS]).toMutableMap()
            starsMap[levelId] = maxOf(starsMap[levelId] ?: 0, stars)
            prefs[Keys.LEVEL_STARS] = encodeIntMap(starsMap)
            if (passed) {
                val highest = prefs[Keys.HIGHEST_LEVEL] ?: 1
                if (levelId >= highest) prefs[Keys.HIGHEST_LEVEL] = levelId + 1
            }
        }
    }

    // ---------------------------------------------------------------
    // Yukseltmeler
    // ---------------------------------------------------------------

    /**
     * Yukseltmeyi TEK atomik islemde yapar: coin yetmiyorsa veya seviye
     * tavandaysa hicbir sey degismez ve false doner.
     */
    suspend fun upgrade(type: UpgradeType): Boolean {
        var success = false
        context.gameDataStore.edit { prefs ->
            val key = upgradeKey(type)
            val level = prefs[key] ?: 1
            val cost = UpgradeCatalog.cost(level) ?: return@edit
            val coins = prefs[Keys.COINS] ?: PlayerProgress.STARTING_COINS
            if (coins >= cost) {
                prefs[Keys.COINS] = coins - cost
                prefs[key] = level + 1
                success = true
            }
        }
        return success
    }

    private fun upgradeKey(type: UpgradeType) = when (type) {
        UpgradeType.SPEED -> Keys.UPGRADE_SPEED
        UpgradeType.ACCELERATION -> Keys.UPGRADE_ACCELERATION
        UpgradeType.BRAKE -> Keys.UPGRADE_BRAKE
        UpgradeType.BOOST -> Keys.UPGRADE_BOOST
    }

    // ---------------------------------------------------------------
    // Guclendiriciler
    // ---------------------------------------------------------------

    /** Coin ile satin alma — harcama ve envanter artisi tek islemde. */
    suspend fun buyBooster(type: BoosterType): Boolean {
        var success = false
        context.gameDataStore.edit { prefs ->
            val coins = prefs[Keys.COINS] ?: PlayerProgress.STARTING_COINS
            if (coins >= type.coinPrice) {
                prefs[Keys.COINS] = coins - type.coinPrice
                val boosters = decodeBoosterMap(prefs[Keys.OWNED_BOOSTERS]).toMutableMap()
                boosters[type] = (boosters[type] ?: 0) + 1
                prefs[Keys.OWNED_BOOSTERS] = encodeBoosterMap(boosters)
                success = true
            }
        }
        return success
    }

    suspend fun addBooster(type: BoosterType, count: Int = 1) {
        context.gameDataStore.edit { prefs ->
            val boosters = decodeBoosterMap(prefs[Keys.OWNED_BOOSTERS]).toMutableMap()
            boosters[type] = (boosters[type] ?: 0) + count
            prefs[Keys.OWNED_BOOSTERS] = encodeBoosterMap(boosters)
        }
    }

    /** Kosu baslarken secilen guclendiricileri envanterden duser. */
    suspend fun consumeBoosters(types: Set<BoosterType>) {
        if (types.isEmpty()) return
        context.gameDataStore.edit { prefs ->
            val boosters = decodeBoosterMap(prefs[Keys.OWNED_BOOSTERS]).toMutableMap()
            types.forEach { type ->
                val owned = boosters[type] ?: 0
                if (owned > 0) boosters[type] = owned - 1
            }
            prefs[Keys.OWNED_BOOSTERS] = encodeBoosterMap(boosters)
        }
    }

    // ---------------------------------------------------------------
    // Arac ozellestirme
    // ---------------------------------------------------------------

    /**
     * Sekil/boya satin alma. Coin dusme, envantere ekleme ve SECME tek atomik
     * `edit {}` blogunda — iki hizli dokunus ayni parayi iki kez harcayamaz.
     * Alinan icerik otomatik secilir (oyuncu ayrica bir tik daha yapmasin).
     */
    suspend fun buyCarShape(id: String): Boolean =
        buyCarItem(
            item = CarCatalog.shapes.firstOrNull { it.id == id },
            ownedKey = Keys.OWNED_CAR_SHAPES,
            selectionKey = Keys.CAR_SHAPE
        )

    suspend fun buyCarColor(id: String): Boolean =
        buyCarItem(
            item = CarCatalog.colors.firstOrNull { it.id == id },
            ownedKey = Keys.OWNED_CAR_COLORS,
            selectionKey = Keys.CAR_COLOR
        )

    private suspend fun buyCarItem(
        item: CarItem?,
        ownedKey: androidx.datastore.preferences.core.Preferences.Key<String>,
        selectionKey: androidx.datastore.preferences.core.Preferences.Key<String>
    ): Boolean {
        if (item == null) return false
        var success = false
        context.gameDataStore.edit { prefs ->
            val owned = decodeStringSet(prefs[ownedKey])
            val coins = prefs[Keys.COINS] ?: PlayerProgress.STARTING_COINS
            val carLevel = PlayerProgress.carLevelForXp(prefs[Keys.XP] ?: 0)
            if (!CarCatalog.canBuy(item, owned, coins, carLevel)) return@edit
            prefs[Keys.COINS] = coins - item.priceCoins
            prefs[ownedKey] = (owned + item.id).joinToString(",")
            prefs[selectionKey] = item.id
            success = true
        }
        return success
    }

    /** Secim yalnizca sahip olunan (veya bedava) bir icerik icin degisir. */
    suspend fun selectCarShape(id: String): Boolean {
        var success = false
        context.gameDataStore.edit { prefs ->
            val owned = decodeStringSet(prefs[Keys.OWNED_CAR_SHAPES])
            val shape = CarCatalog.shapes.firstOrNull { it.id == id } ?: return@edit
            if (!CarCatalog.isOwned(shape, owned)) return@edit
            prefs[Keys.CAR_SHAPE] = shape.id
            success = true
        }
        return success
    }

    suspend fun selectCarColor(id: String): Boolean {
        var success = false
        context.gameDataStore.edit { prefs ->
            val owned = decodeStringSet(prefs[Keys.OWNED_CAR_COLORS])
            val color = CarCatalog.colors.firstOrNull { it.id == id } ?: return@edit
            if (!CarCatalog.isOwned(color, owned)) return@edit
            prefs[Keys.CAR_COLOR] = color.id
            success = true
        }
        return success
    }

    // ---------------------------------------------------------------
    // Sonsuz mod
    // ---------------------------------------------------------------

    suspend fun recordEndlessRun(seconds: Int, score: Int) {
        context.gameDataStore.edit { prefs ->
            if (seconds > (prefs[Keys.ENDLESS_BEST_SECONDS] ?: 0)) {
                prefs[Keys.ENDLESS_BEST_SECONDS] = seconds
            }
            if (score > (prefs[Keys.ENDLESS_BEST_SCORE] ?: 0)) {
                prefs[Keys.ENDLESS_BEST_SCORE] = score
            }
        }
    }

    // ---------------------------------------------------------------
    // Ayarlar
    // ---------------------------------------------------------------

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.gameDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.gameDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    /**
     * Ilk acilistaki dil ekranindan gelen secim: dil ve "secildi" bayragi TEK
     * atomik islemde yazilir — biri yazilip digeri yazilamazsa ekran tekrar
     * cikardi ya da dil kaydedilmemis olurdu.
     */
    suspend fun chooseLanguage(language: AppLanguage) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
            prefs[Keys.LANGUAGE_CHOSEN] = true
        }
    }

    suspend fun markOnboardingSeen() {
        context.gameDataStore.edit { it[Keys.HAS_SEEN_ONBOARDING] = true }
    }

    // ---------------------------------------------------------------
    // Reklam sayaclari
    // ---------------------------------------------------------------

    suspend fun incrementLevelsSinceInterstitial() {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.LEVELS_SINCE_INTERSTITIAL] = (prefs[Keys.LEVELS_SINCE_INTERSTITIAL] ?: 0) + 1
        }
    }

    suspend fun resetLevelsSinceInterstitial() {
        context.gameDataStore.edit { it[Keys.LEVELS_SINCE_INTERSTITIAL] = 0 }
    }

    suspend fun incrementEndlessRunsSinceInterstitial() {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.ENDLESS_RUNS_SINCE_INTERSTITIAL] =
                (prefs[Keys.ENDLESS_RUNS_SINCE_INTERSTITIAL] ?: 0) + 1
        }
    }

    suspend fun resetEndlessRunsSinceInterstitial() {
        context.gameDataStore.edit { it[Keys.ENDLESS_RUNS_SINCE_INTERSTITIAL] = 0 }
    }

    // ---------------------------------------------------------------
    // Haftalik gorevler
    // ---------------------------------------------------------------

    suspend fun incrementMissionProgress(type: MissionType, amount: Int) {
        if (amount <= 0) return
        context.gameDataStore.edit { prefs ->
            val weekId = currentWeekId()
            val storedWeekId = prefs[Keys.MISSION_WEEK_ID]
            val sameWeek = storedWeekId == weekId
            val progress = if (sameWeek) {
                decodeStringIntMap(prefs[Keys.MISSION_PROGRESS]).toMutableMap()
            } else {
                mutableMapOf()
            }
            WeeklyMissionGenerator.forWeek(weekId)
                .filter { it.type == type }
                .forEach { mission ->
                    val current = progress[mission.id] ?: 0
                    progress[mission.id] = (current + amount).coerceAtMost(mission.tiers.last().target)
                }
            prefs[Keys.MISSION_WEEK_ID] = weekId
            prefs[Keys.MISSION_PROGRESS] = encodeStringIntMap(progress)
            if (!sameWeek) prefs[Keys.MISSION_CLAIMED] = ""
        }
    }

    /** Bir kademe odulunu talep eder (yalnizca bir kez). */
    suspend fun claimMissionTier(missionId: String, tierIndex: Int, rewardCoins: Int) {
        context.gameDataStore.edit { prefs ->
            val claimed = decodeStringSet(prefs[Keys.MISSION_CLAIMED]).toMutableSet()
            if (claimed.add("$missionId#$tierIndex")) {
                prefs[Keys.MISSION_CLAIMED] = claimed.joinToString(",")
                prefs[Keys.COINS] = (prefs[Keys.COINS] ?: PlayerProgress.STARTING_COINS) + rewardCoins
            }
        }
    }

    /** Haftalik sandik: coin + bir guclendirici, haftada bir kez. */
    suspend fun claimWeeklyChest(): Boolean {
        var success = false
        context.gameDataStore.edit { prefs ->
            val claimed = decodeStringSet(prefs[Keys.MISSION_CLAIMED]).toMutableSet()
            if (claimed.add(WeeklyMissionGenerator.CHEST_CLAIM_KEY)) {
                prefs[Keys.MISSION_CLAIMED] = claimed.joinToString(",")
                prefs[Keys.COINS] = (prefs[Keys.COINS] ?: PlayerProgress.STARTING_COINS) +
                    WeeklyMissionGenerator.WEEKLY_CHEST_COINS
                val boosters = decodeBoosterMap(prefs[Keys.OWNED_BOOSTERS]).toMutableMap()
                val type = WeeklyMissionGenerator.WEEKLY_CHEST_BOOSTER
                boosters[type] = (boosters[type] ?: 0) + 1
                prefs[Keys.OWNED_BOOSTERS] = encodeBoosterMap(boosters)
                success = true
            }
        }
        return success
    }

    // ---------------------------------------------------------------
    // Gunluk gorev
    // ---------------------------------------------------------------

    /**
     * Gunluk gorevde [tiersReached] kademeye ulasildi: yalnizca DAHA ONCE
     * ODENMEMIS kademelerin odulu eklenir ve odenen coin dondurulur (0 =
     * yeni kademe yok). Okuma ve yazma tek `edit {}` blogunda — iki kosu
     * ust uste bitse bile ayni kademe iki kez odenmez.
     */
    suspend fun grantDailyTiers(dayId: String, challenge: DailyChallenge, tiersReached: Int): Int {
        var granted = 0
        context.gameDataStore.edit { prefs ->
            val tierCount = challenge.tiers.size
            val already = grantedTiers(prefs, dayId, tierCount)
            val reached = tiersReached.coerceIn(0, tierCount)
            if (reached > already) {
                granted = challenge.rewardBetween(already, reached)
                // Kayitli gun ileriyse (saat geri alinmis) onu KORU: aksi
                // halde her geri alma gunluk gorevi yeniden odenebilir yapardi.
                if (isFreshDay(prefs[Keys.DAILY_DAY_ID], dayId)) {
                    prefs[Keys.DAILY_DAY_ID] = dayId
                }
                prefs[Keys.DAILY_TIER] = reached
                prefs[Keys.COINS] = (prefs[Keys.COINS] ?: PlayerProgress.STARTING_COINS) + granted
            }
        }
        return granted
    }

    companion object {
        /**
         * Gunluk sayaclar SADECE ILERI giden tarihte sifirlanir.
         *
         * "yyyy-MM-dd" sozluksel olarak da kronolojik siralanir, yani
         * `today > stored` gecerli bir "yeni gun" testidir. Cihaz saatini
         * GERI almak eskiden gunluk gorevi ve odullu reklam limitini
         * sifirliyordu — sinirsiz odul demekti (2026-08-14). Kayitli gun
         * bugunden ileriyse sayac KORUNUR.
         *
         * Not: bu, saati ILERI alip sonra beklemeyi engellemez; sunucusuz bir
         * oyunda tam koruma yok. Kolay ve tekrarlanabilir olan yol kapatildi.
         *
         * Companion'da: saf fonksiyon, JVM testinden dogrudan cagrilabilsin.
         */
        fun isFreshDay(storedDay: String?, today: String): Boolean =
            storedDay == null || today > storedDay

        fun currentWeekId(): String {
            val calendar = Calendar.getInstance()
            return "${calendar.get(Calendar.YEAR)}-W${calendar.get(Calendar.WEEK_OF_YEAR)}"
        }

        private fun encodeIntMap(map: Map<Int, Int>): String =
            map.entries.joinToString(",") { "${it.key}:${it.value}" }

        private fun decodeIntMap(raw: String?): Map<Int, Int> {
            if (raw.isNullOrBlank()) return emptyMap()
            return raw.split(",").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    parts[0].toIntOrNull()?.let { k -> parts[1].toIntOrNull()?.let { v -> k to v } }
                } else {
                    null
                }
            }.toMap()
        }

        private fun encodeBoosterMap(map: Map<BoosterType, Int>): String =
            map.entries.filter { it.value > 0 }.joinToString(",") { "${it.key.name}:${it.value}" }

        private fun decodeBoosterMap(raw: String?): Map<BoosterType, Int> {
            if (raw.isNullOrBlank()) return emptyMap()
            return raw.split(",").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val type = runCatching { BoosterType.valueOf(parts[0]) }.getOrNull()
                    val count = parts[1].toIntOrNull()
                    if (type != null && count != null) type to count else null
                } else {
                    null
                }
            }.toMap()
        }

        private fun encodeStringIntMap(map: Map<String, Int>): String =
            map.entries.joinToString(",") { "${it.key}:${it.value}" }

        private fun decodeStringIntMap(raw: String?): Map<String, Int> {
            if (raw.isNullOrBlank()) return emptyMap()
            return raw.split(",").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) parts[1].toIntOrNull()?.let { v -> parts[0] to v } else null
            }.toMap()
        }

        private fun decodeStringSet(raw: String?): Set<String> {
            if (raw.isNullOrBlank()) return emptySet()
            return raw.split(",").filter { it.isNotBlank() }.toSet()
        }
    }
}
