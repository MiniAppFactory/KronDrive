package com.miniappfactory.krondrive.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miniappfactory.krondrive.game.CarCatalog
import com.miniappfactory.krondrive.game.CarShapeDef
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

        /**
         * ⚠ GECICI TEST ANAHTARI — [PlayerProgress.STARTING_COINS] ile birlikte
         * kaldirilacak. Test coininin BIR KEZ verildigini isaretler.
         */
        val TEST_COINS_GRANTED = androidx.datastore.preferences.core.booleanPreferencesKey(
            "test_coins_granted"
        )
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

        /**
         * ESKI tek boya secimi. v1.0.9'dan itibaren yerini
         * [CAR_COLOR_BY_SHAPE] aldi; hala YAZILIYOR ve surum yukselten
         * oyuncunun rengini kaybetmemek icin okunuyor.
         */
        val CAR_COLOR = stringPreferencesKey("car_color")

        /**
         * Gövde -> boya. Oyuncu her araci ayri boyayabilsin ve gövde
         * degistirince o aracin rengi geri gelsin diye (2026-08-15).
         * Bicim: `shapeId:colorId,shapeId:colorId`.
         */
        val CAR_COLOR_BY_SHAPE = stringPreferencesKey("car_color_by_shape")
        val OWNED_CAR_SHAPES = stringPreferencesKey("owned_car_shapes")
        val OWNED_CAR_COLORS = stringPreferencesKey("owned_car_colors")

        val ENDLESS_BEST_SECONDS = intPreferencesKey("endless_best_seconds")
        val ENDLESS_BEST_SCORE = intPreferencesKey("endless_best_score")

        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")

        /**
         * Titresim tercihi; 2026-08-17'de (v1.0.9 sonrasi) eklendi. Eski
         * kayitlarda YOK ve okumasi bilerek `?: true` — titresim bu
         * anahtardan once kosulsuz calisiyordu, varsayilan kapali olsaydi
         * guncelleyen oyuncunun oyunu sessizce degisirdi.
         */
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
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

    /**
     * ⚠⚠ GECICI — [PlayerProgress.STARTING_COINS] test degerindeyken calisir,
     * yayin degerine donuldugu an KENDILIGINDEN olur ve hicbir sey yapmaz. ⚠⚠
     *
     * Sorun neydi: `STARTING_COINS` yalnizca TEMIZ KURULUMDA gecerli — DataStore
     * bir kez "coins" yazdiktan sonra varsayilan hic okunmuyor. Sahibi test
     * APK'sini mevcut profilin uzerine kurunca 100.000 coin HIC GELMEDI, eski
     * 100 coin duruyordu (2026-08-17'de cihazda goruldu).
     *
     * Cozum tek seferlik yukleme. Neden bayrak var: her aciliste yukleseydi
     * oyuncunun harcamasi geri gelirdi ve ekonomiyi test etmek imkansiz olurdu
     * — arac denemek icin coin gerekiyor ama harcamanin da bir sonucu olmali.
     *
     * Bayrak DataStore'da; uygulama verisi silinmedikce ikinci kez calismaz.
     */
    suspend fun grantTestCoinsOnce() {
        if (PlayerProgress.STARTING_COINS == PlayerProgress.STARTING_COINS_RELEASE) return
        context.gameDataStore.edit { prefs ->
            if (prefs[Keys.TEST_COINS_GRANTED] == true) return@edit
            prefs[Keys.TEST_COINS_GRANTED] = true
            val mevcut = prefs[Keys.COINS] ?: 0
            if (mevcut < PlayerProgress.STARTING_COINS) {
                prefs[Keys.COINS] = PlayerProgress.STARTING_COINS
            }
        }
    }

    val playerProgress: Flow<PlayerProgress> = context.gameDataStore.data.map { prefs ->
        val ownedShapes = decodeStringSet(prefs[Keys.OWNED_CAR_SHAPES])
        // Sahip olunan her gövdenin fabrika boyasi da acik sayilir
        // (bkz. CarShapeDef.defaultColorId) — kural tek yerde, katalogda.
        val ownedColors = CarCatalog.effectiveOwnedColors(
            ownedShapes = ownedShapes,
            ownedColors = decodeStringSet(prefs[Keys.OWNED_CAR_COLORS])
        )
        val shape = CarCatalog.selectedShape(prefs[Keys.CAR_SHAPE], ownedShapes)
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
            // Anahtari olmayan (guncelleme oncesi) kayit icin ACIK — bkz.
            // Keys.VIBRATION_ENABLED.
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
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
            carShapeId = shape.id,
            // Boya SECILI GOVDEYE gore cozulur: oyuncu o gövde icin bir boya
            // sectiyse o, yoksa gövdenin fabrika boyasi. Eski tek anahtarli
            // kayit ([Keys.CAR_COLOR]) hala okunuyor — surum yukselten
            // oyuncunun rengi kaybolmasin diye (bkz. colorIdFor).
            carColorId = colorIdFor(prefs, shape, ownedColors),
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
        // NEGATIF/SIFIR TUTAR REDDEDILIR (2026-08-19).
        //
        // Guard yoktu ve kardesi [addCoins]'de vardi. `spendCoins(-100)`
        // cagrisi `current >= -100` kontrolunden gecip `current - (-100)`
        // yaziyordu: yani HARCAMA fonksiyonu coin EKLIYOR ve `true`
        // donuyordu. Bugun cagirani yok, yani su an ulasilamaz — ama bir
        // sonraki kullanan icin hazir bir tuzak, ve "harcadim" diyen bir
        // fonksiyonun para basmasi sessizce olur.
        if (amount <= 0) return false

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
     * Bolum sonucu. [passed] gorevlerin [GameConfig.MIN_STARS_TO_PASS]
     * kadari tamamlandiysa true gelir (su an UC gorevin IKISI) ve bir
     * sonraki bolumun kilidi ancak o zaman acilir.
     *
     * Esik iki kez degisti: once `stars > 0`, sonra "ucu de" (2026-08-15,
     * sahibi karari), sonra 2 (2026-08-16, olcum: "ucu de" ilk sekiz bolumde
     * duvar uretiyordu — bkz. docs/DIFFICULTY_REVIEW.md).
     * Yildizlar SIRALI kazanildigi icin bu pratikte "ILK IKI gorev" demek.
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
        // Negatif sayi reddedilir (2026-08-19). Kardes fonksiyonlarin
        // ([addCoins], [addXp], [incrementMissionProgress]) hepsinde bu guard
        // vardi, burada yoktu: `addBooster(type, -5)` booster sayisini
        // NEGATIFE dusururdu ve sahip olunmayan booster kullanilabilir hale
        // gelirdi. Bugun yalnizca +1 ile cagriliyor, yani ulasilamaz — ayni
        // aileden `spendCoins` tuzagiyla birlikte kapatildi.
        if (count <= 0) return

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
            // Boya yazilmiyor: yeni arac kendi fabrika boyasiyla gelir
            // (okuma sirasinda cozuluyor, bkz. colorIdFor).
            select = { prefs, itemId -> prefs[Keys.CAR_SHAPE] = itemId }
        )

    suspend fun buyCarColor(id: String): Boolean =
        buyCarItem(
            item = CarCatalog.colors.firstOrNull { it.id == id },
            ownedKey = Keys.OWNED_CAR_COLORS,
            // Boya secimi gövdeye ozel haritaya da yazilmali; yoksa satin
            // alinan renk bir sonraki okumada geri duserdi.
            select = { prefs, itemId -> applyColorSelection(prefs, itemId) }
        )

    private suspend fun buyCarItem(
        item: CarItem?,
        ownedKey: androidx.datastore.preferences.core.Preferences.Key<String>,
        select: (androidx.datastore.preferences.core.MutablePreferences, String) -> Unit
    ): Boolean {
        if (item == null) return false
        var success = false
        context.gameDataStore.edit { prefs ->
            val owned = decodeStringSet(prefs[ownedKey])
            val coins = prefs[Keys.COINS] ?: PlayerProgress.STARTING_COINS
            val carLevel = PlayerProgress.carLevelForXp(prefs[Keys.XP] ?: 0)
            if (!CarCatalog.canBuy(item, owned, coins, carLevel)) return@edit
            // FIYAT DEGIL TOPLAM: seviye yetmiyorsa atlama bedeli de dusulur
            // (2026-08-19). `item.priceCoins` yazsaydik oyuncu seviyeyi
            // BEDAVAYA atlardi — kontrol `canBuy` toplama bakiyor ama tahsilat
            // fiyata bakiyor olurdu, yani klasik "kontrol bir yerde, etki
            // baska yerde" hatasi.
            prefs[Keys.COINS] = coins - CarCatalog.totalPrice(item, carLevel)
            prefs[ownedKey] = (owned + item.id).joinToString(",")
            select(prefs, item.id)
            success = true
        }
        return success
    }

    /**
     * Secim yalnizca sahip olunan (veya bedava) bir icerik icin degisir.
     *
     * Boya YAZILMAZ: hangi boyanin gecerli oldugu okuma sirasinda gövdeye
     * gore cozuluyor ([colorIdFor]). Yani gövde degistirmek o aracin en son
     * boyasini — hic boyanmadiysa fabrika boyasini — kendiliginden getirir.
     */
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
            val owned = CarCatalog.effectiveOwnedColors(
                ownedShapes = decodeStringSet(prefs[Keys.OWNED_CAR_SHAPES]),
                ownedColors = decodeStringSet(prefs[Keys.OWNED_CAR_COLORS])
            )
            val color = CarCatalog.colors.firstOrNull { it.id == id } ?: return@edit
            if (!CarCatalog.isOwned(color, owned)) return@edit
            applyColorSelection(prefs, color.id)
            success = true
        }
        return success
    }

    /** Boyayi hem gövdeye ozel haritaya hem de eski anahtara yazar. */
    private fun applyColorSelection(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        colorId: String
    ) {
        val shapeId = CarCatalog.selectedShape(
            prefs[Keys.CAR_SHAPE],
            decodeStringSet(prefs[Keys.OWNED_CAR_SHAPES])
        ).id
        val map = decodeStringMap(prefs[Keys.CAR_COLOR_BY_SHAPE]).toMutableMap()
        map[shapeId] = colorId
        prefs[Keys.CAR_COLOR_BY_SHAPE] = encodeStringMap(map)
        prefs[Keys.CAR_COLOR] = colorId
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

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.gameDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
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

        // KODLAMA MANTIGI [SaveCodec]'e TASINDI (2026-08-19).
        //
        // Buradaki fonksiyonlar `private` oldugu ve iceren sinif `Context`
        // istedigi icin JVM testinden erisilemiyorlardi — yani oyunun kalici
        // verisini yazip okuyan kod tamamen test disindaydi. Govdeler saf
        // Kotlin bir nesneye tasindi; buradakiler yalnizca yonlendirme, cagri
        // yerleri degismedi.
        private fun encodeIntMap(map: Map<Int, Int>) = SaveCodec.encodeIntMap(map)
        private fun decodeIntMap(raw: String?) = SaveCodec.decodeIntMap(raw)
        private fun encodeBoosterMap(map: Map<BoosterType, Int>) =
            SaveCodec.encodeBoosterMap(map)
        private fun decodeBoosterMap(raw: String?) = SaveCodec.decodeBoosterMap(raw)
        private fun encodeStringIntMap(map: Map<String, Int>) =
            SaveCodec.encodeStringIntMap(map)
        private fun decodeStringIntMap(raw: String?) = SaveCodec.decodeStringIntMap(raw)
        private fun decodeStringSet(raw: String?) = SaveCodec.decodeStringSet(raw)
        private fun encodeStringMap(map: Map<String, String>) = SaveCodec.encodeStringMap(map)
        private fun decodeStringMap(raw: String?) = SaveCodec.decodeStringMap(raw)

        /**
         * Secili gövdenin boyasi. Oncelik sirasi:
         *
         *  1. O gövde icin kayitli secim ([Keys.CAR_COLOR_BY_SHAPE]);
         *  2. ESKI tek boya kaydi — ama YALNIZCA fabrika boyasi varsayilan
         *     olan gövdelerde. Kuş SLX (petrol) ve Dağ Keçisi (beyaz) bunun
         *     disinda tutuldu: surum yukselten oyuncuda da bu iki arac
         *     tasarlandigi renkte gorunsun (proje sahibi karari, 2026-08-15);
         *  3. gövdenin fabrika boyasi.
         *
         * Sahip olunmayan bir boya secilmis gorunuyorsa yine fabrika boyasina
         * duser — kayit bozulmasi oyunu kirmasin.
         */
        private fun colorIdFor(
            prefs: androidx.datastore.preferences.core.Preferences,
            shape: CarShapeDef,
            ownedColors: Set<String>
        ): String {
            val perShape = decodeStringMap(prefs[Keys.CAR_COLOR_BY_SHAPE])[shape.id]
            val legacy = prefs[Keys.CAR_COLOR]
                ?.takeIf { shape.defaultColorId == CarCatalog.DEFAULT_COLOR_ID }
            val candidate = perShape ?: legacy ?: shape.defaultColorId
            val resolved = CarCatalog.colors.firstOrNull {
                it.id == candidate && CarCatalog.isOwned(it, ownedColors)
            }
            return (resolved ?: CarCatalog.color(shape.defaultColorId)).id
        }
    }
}
