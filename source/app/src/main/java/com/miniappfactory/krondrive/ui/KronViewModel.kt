package com.miniappfactory.krondrive.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniappfactory.krondrive.data.AppLanguage
import com.miniappfactory.krondrive.data.BoosterType
import com.miniappfactory.krondrive.data.DailyChallenge
import com.miniappfactory.krondrive.data.DailyChallengeGenerator
import com.miniappfactory.krondrive.data.DailyChallengeState
import com.miniappfactory.krondrive.data.GameStateRepository
import com.miniappfactory.krondrive.data.MissionType
import com.miniappfactory.krondrive.data.PlayerProgress
import com.miniappfactory.krondrive.data.WeeklyMissionGenerator
import com.miniappfactory.krondrive.data.WeeklyMissionProgress
import com.miniappfactory.krondrive.game.GameConfig
import com.miniappfactory.krondrive.game.GameEngine
import com.miniappfactory.krondrive.game.LevelCatalog
import com.miniappfactory.krondrive.game.RunMode
import com.miniappfactory.krondrive.game.RunResult
import com.miniappfactory.krondrive.game.UpgradeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tum ekranlarin paylastigi tek ViewModel (Boom Blocks'taki BlastViewModel
 * ile ayni rol). Oyun simulasyonunu TUTMAZ — o [GameEngine] icinde yasar ve
 * oyun ekranina aittir; burasi kalici ilerlemeyi ve mod secimlerini yonetir.
 */
class KronViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameStateRepository(application)

    val playerProgress: StateFlow<PlayerProgress> = repository.playerProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerProgress())

    val weeklyMissions: StateFlow<WeeklyMissionProgress> = repository.weeklyMissionProgress
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            run {
                val weekId = GameStateRepository.currentWeekId()
                WeeklyMissionProgress(weekId, WeeklyMissionGenerator.forWeek(weekId))
            }
        )

    val dailyChallenge: StateFlow<DailyChallengeState> = repository.dailyChallengeState
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            run {
                val dayId = DailyChallengeGenerator.currentDayId()
                DailyChallengeState(dayId, DailyChallengeGenerator.forDay(dayId))
            }
        )

    /** Kosu baslamadan once secilen guclendiriciler (envanterden dusulur). */
    private val _selectedBoosters = MutableStateFlow<Set<BoosterType>>(emptySet())
    val selectedBoosters: StateFlow<Set<BoosterType>> = _selectedBoosters.asStateFlow()

    /** Son biten kosunun sonucu — sonuc ekrani bunu okur. */
    private val _lastResult = MutableStateFlow<RunResult?>(null)
    val lastResult: StateFlow<RunResult?> = _lastResult.asStateFlow()

    /**
     * Son gunluk gorev kosusunda GERCEKTEN odenen coin. Sonuc ekrani bunu
     * gosterir; ulasilan kademeden hesaplanamaz cunku ayni kademe gun icinde
     * ikinci kez odenmez (o durumda 0 doner).
     */
    private val _lastDailyReward = MutableStateFlow(0)
    val lastDailyReward: StateFlow<Int> = _lastDailyReward.asStateFlow()

    val language: AppLanguage get() = playerProgress.value.language

    // -----------------------------------------------------------------
    // Kosu hazirligi
    // -----------------------------------------------------------------

    fun toggleBooster(type: BoosterType) {
        val owned = playerProgress.value.boosterCount(type)
        _selectedBoosters.value = _selectedBoosters.value.toMutableSet().apply {
            if (!remove(type) && owned > 0) add(type)
        }
    }

    fun clearSelectedBoosters() {
        _selectedBoosters.value = emptySet()
    }

    /**
     * Secilen moda gore motoru kurar. Secili guclendiriciler burada envanterden
     * DUSULUR — yani kosu basladigi an harcanmis sayilir (kullanici oyundan
     * cikip tekrar girerek ayni guclendiriciyi iki kez kullanamaz).
     */
    fun createEngine(mode: RunMode, levelId: Int? = null): GameEngine {
        val progress = playerProgress.value
        val boosters = _selectedBoosters.value.filter { progress.boosterCount(it) > 0 }.toSet()
        val level = when (mode) {
            RunMode.CAREER -> levelId?.let { LevelCatalog.level(it) }
            RunMode.DAILY -> dailyChallenge.value.challenge.toLevelDef()
            RunMode.ENDLESS -> null
        }
        if (boosters.isNotEmpty()) {
            viewModelScope.launch { repository.consumeBoosters(boosters) }
        }
        _selectedBoosters.value = emptySet()
        return GameEngine(
            mode = mode,
            level = level,
            upgrades = progress.upgrades,
            boosters = boosters,
            endlessRecordSeconds = progress.endlessBestSeconds,
            // Bu bolumden daha once kazanilmis yildizlar: coin odulu yalnizca
            // bunun ustune cikan yildizlar icin verilir (farm engeli).
            previousStars = if (mode == RunMode.CAREER && levelId != null) {
                progress.starsOf(levelId)
            } else {
                0
            },
            carStyle = progress.carStyle
        )
    }

    fun currentDailyChallenge(): DailyChallenge = dailyChallenge.value.challenge

    // -----------------------------------------------------------------
    // Kosu sonucu
    // -----------------------------------------------------------------

    /**
     * Kosu bittiginde TUM kalici etkiler burada uygulanir: coin/XP, yildizlar,
     * bolum acilisi, haftalik gorev sayaclari, sonsuz mod rekoru, gunluk gorev
     * odulu ve reklam sayaclari.
     */
    fun onRunFinished(result: RunResult) {
        _lastResult.value = result
        _lastDailyReward.value = 0
        val stats = result.stats
        viewModelScope.launch {
            repository.addCoins(result.coinsEarned)
            repository.addXp(result.xpEarned)

            // Haftalik gorevler her modda ilerler (oyuncu ne oynarsa oynasin).
            repository.incrementMissionProgress(MissionType.DRIVE_DISTANCE, stats.distanceMeters)
            repository.incrementMissionProgress(MissionType.PASS_VEHICLES, stats.vehiclesPassed)
            repository.incrementMissionProgress(MissionType.PERFECT_DODGES, stats.perfectDodges)
            repository.incrementMissionProgress(MissionType.BIG_COMBOS, stats.bigCombos)

            when (result.mode) {
                RunMode.CAREER -> {
                    val levelId = result.levelId
                    if (levelId != null && stats.completed) {
                        repository.recordLevelResult(levelId, result.stars)
                        repository.incrementMissionProgress(MissionType.COMPLETE_LEVELS, 1)
                        repository.incrementLevelsSinceInterstitial()
                    }
                }

                RunMode.ENDLESS -> {
                    repository.recordEndlessRun(stats.timeSurvivedSec, stats.score)
                    repository.incrementEndlessRunsSinceInterstitial()
                }

                RunMode.DAILY -> {
                    // Kademeler carpismadan bagimsiz sayilir; `completed`
                    // sartina bagli DEGIL (bkz. LevelEvaluator.tiersReached).
                    val state = dailyChallenge.value
                    _lastDailyReward.value = repository.grantDailyTiers(
                        dayId = state.dayId,
                        challenge = state.challenge,
                        tiersReached = result.dailyTiers
                    )
                }
            }
        }
    }

    fun clearLastResult() {
        _lastResult.value = null
        _lastDailyReward.value = 0
    }

    // -----------------------------------------------------------------
    // Reklam frekansi
    // -----------------------------------------------------------------

    /**
     * Bolum/kosu sonrasi gecis reklami gosterilmeli mi. Esik [GameConfig]
     * icinde tanimli — her bolumden sonra reklam GOSTERILMEZ.
     */
    fun shouldShowInterstitial(mode: RunMode): Boolean {
        val progress = playerProgress.value
        return when (mode) {
            RunMode.CAREER, RunMode.DAILY ->
                progress.levelsSinceInterstitial >= GameConfig.INTERSTITIAL_EVERY_N_LEVELS

            RunMode.ENDLESS ->
                progress.endlessRunsSinceInterstitial >= GameConfig.INTERSTITIAL_EVERY_N_ENDLESS_RUNS
        }
    }

    /**
     * "Tekrar dene" sayaci. Kalici DEGIL (bellekte tutuluyor): uygulama
     * yeniden acildiginda sifirlanmasi zararsiz, buna karsilik karar anlik
     * verilebiliyor — reklam gosterip gostermeme kararini asenkron bir
     * DataStore okumasina baglamak gereksiz karmasiklik olurdu.
     */
    private var retriesSinceInterstitial = 0

    /**
     * Tekrar denemeyi sayar ve bu denemede gecis reklami gosterilip
     * gosterilmeyecegini soyler (esik: [GameConfig.INTERSTITIAL_EVERY_N_RETRIES]).
     */
    fun consumeRetryInterstitial(): Boolean {
        retriesSinceInterstitial++
        return if (retriesSinceInterstitial >= GameConfig.INTERSTITIAL_EVERY_N_RETRIES) {
            retriesSinceInterstitial = 0
            true
        } else {
            false
        }
    }

    fun onInterstitialShown(mode: RunMode) {
        viewModelScope.launch {
            when (mode) {
                RunMode.CAREER, RunMode.DAILY -> repository.resetLevelsSinceInterstitial()
                RunMode.ENDLESS -> repository.resetEndlessRunsSinceInterstitial()
            }
        }
    }

    // -----------------------------------------------------------------
    // Garaj / magaza
    // -----------------------------------------------------------------

    fun upgrade(type: UpgradeType) {
        viewModelScope.launch { repository.upgrade(type) }
    }

    fun buyBooster(type: BoosterType) {
        viewModelScope.launch { repository.buyBooster(type) }
    }

    // --- Arac ozellestirme ---
    //
    // Fiyat/seviye kontrolu repository'de ATOMIK yapilir; ekran yalnizca niyet
    // bildirir. Alinan icerik satin alma isleminin kendisinde secilir.

    fun buyCarShape(id: String) {
        viewModelScope.launch { repository.buyCarShape(id) }
    }

    fun selectCarShape(id: String) {
        viewModelScope.launch { repository.selectCarShape(id) }
    }

    fun buyCarColor(id: String) {
        viewModelScope.launch { repository.buyCarColor(id) }
    }

    fun selectCarColor(id: String) {
        viewModelScope.launch { repository.selectCarColor(id) }
    }

    /**
     * Odullu reklam gercekten izlendiginde cagrilir (SDK'nin "kazanildi"
     * callback'i). Gunluk sinir repository tarafinda kontrol edilir.
     */
    fun grantRewardedCoins() {
        viewModelScope.launch { repository.grantRewardedCoins() }
    }

    /**
     * Sonuc ekranindaki "reklam izle, odulu ikiye katla" icin.
     *
     * Eskiden dogrudan [GameStateRepository.addCoins] cagiriyordu: hicbir
     * gunluk siniri yoktu ve ayni kosu tekrarlanarak sinirsiz coin
     * basilabiliyordu. Artik garajdaki reklamla AYNI gunluk sayaci kullanir
     * ve tek seferde en fazla [GameConfig.REWARDED_DOUBLE_COINS_CAP] oder.
     * Sinir dolmussa false doner ve ekran bunu soyler.
     */
    fun grantBonusCoins(amount: Int, onResult: (granted: Int) -> Unit = {}) {
        viewModelScope.launch {
            val payout = amount.coerceAtMost(GameConfig.REWARDED_DOUBLE_COINS_CAP)
            val success = repository.grantRewardedCoins(payout)
            onResult(if (success) payout else 0)
        }
    }

    // -----------------------------------------------------------------
    // Gorevler
    // -----------------------------------------------------------------

    fun claimMissionTier(missionId: String, tierIndex: Int) {
        viewModelScope.launch {
            val mission = weeklyMissions.value.missions.firstOrNull { it.id == missionId }
                ?: return@launch
            val tier = mission.tiers.getOrNull(tierIndex) ?: return@launch
            val progress = repository.weeklyMissionProgress.first()
            if (progress.progressOf(mission) < tier.target) return@launch
            repository.claimMissionTier(missionId, tierIndex, tier.rewardCoins)
        }
    }

    fun claimWeeklyChest() {
        viewModelScope.launch {
            val progress = repository.weeklyMissionProgress.first()
            if (progress.allTiersComplete()) repository.claimWeeklyChest()
        }
    }

    // -----------------------------------------------------------------
    // Ayarlar
    // -----------------------------------------------------------------

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundEnabled(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language) }
    }

    fun markOnboardingSeen() {
        viewModelScope.launch { repository.markOnboardingSeen() }
    }
}
