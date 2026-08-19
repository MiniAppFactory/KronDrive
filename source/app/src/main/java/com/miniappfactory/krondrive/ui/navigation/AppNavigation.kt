package com.miniappfactory.krondrive.ui.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.miniappfactory.krondrive.ads.AdConsentGate
import com.miniappfactory.krondrive.ads.ConsentManager
import com.miniappfactory.krondrive.ads.RewardedAdManager
import com.miniappfactory.krondrive.game.RunMode
import com.miniappfactory.krondrive.ui.KronViewModel
import com.miniappfactory.krondrive.ui.game.GameScreen
import com.miniappfactory.krondrive.ui.garage.GarageScreen
import com.miniappfactory.krondrive.ui.levels.LevelMapScreen
import com.miniappfactory.krondrive.ui.menu.MainMenuScreen
import com.miniappfactory.krondrive.ui.missions.MissionsScreen
import com.miniappfactory.krondrive.ui.onboarding.LanguageGateScreen
import com.miniappfactory.krondrive.ui.settings.SettingsScreen

private object Routes {
    const val MENU = "menu"
    const val CAREER = "career"
    const val GARAGE = "garage"
    const val MISSIONS = "missions"
    const val SETTINGS = "settings"

    /** Oyun ekrani: mod + (kariyerde) bolum numarasi. Kariyer disinda level = -1. */
    const val GAME = "game/{mode}/{level}/{training}"

    /**
     * [training] = ANTRENMAN modu: trafik yalnizca yan seritlerde dogar.
     * Varsayilan **false** — normal menuler gercek oyunu acar.
     */
    fun game(mode: RunMode, level: Int?, training: Boolean = false) =
        "game/${mode.name}/${level ?: -1}/$training"
}

/**
 * Ekran gecisleri. Oyun ekrani her zaman menuye/bolum haritasina DONER
 * (backstack'te birikmez) — aksi halde 10 bolum ust uste oynayinca geri tusu
 * oyuncuyu eski kosulara goturur.
 */
@Composable
fun AppNavigation(viewModel: KronViewModel, adsConsentResolved: Boolean) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as? Activity
    val progress by viewModel.playerProgress.collectAsStateWithLifecycle()
    val progressLoaded by viewModel.progressLoaded.collectAsStateWithLifecycle()

    // Kayit okunana kadar hicbir sey cizilmez: aksi halde mevcut oyuncuya da
    // bir an dil ekrani gorunurdu (varsayilan languageChosen = false).
    if (!progressLoaded) return

    // Ilk acilis: dil secilmeden menuye girilmez. NavHost'un DISINDA duruyor —
    // geri tusuyla donulebilen bir ekran degil, bir kapi.
    if (!progress.languageChosen) {
        LanguageGateScreen(onChoose = viewModel::chooseLanguage)
        return
    }
    val missions by viewModel.weeklyMissions.collectAsStateWithLifecycle()
    val daily by viewModel.dailyChallenge.collectAsStateWithLifecycle()
    val selectedBoosters by viewModel.selectedBoosters.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Routes.MENU) {
        composable(Routes.MENU) {
            MainMenuScreen(
                progress = progress,
                daily = daily,
                missionsHaveClaimable = missions.hasClaimable(),
                adsConsentResolved = adsConsentResolved,
                onCareer = { navController.navigate(Routes.CAREER) },
                onEndless = { navController.navigate(Routes.game(RunMode.ENDLESS, null)) },
                onTraining = {
                    navController.navigate(Routes.game(RunMode.ENDLESS, null, training = true))
                },
                onDaily = { navController.navigate(Routes.game(RunMode.DAILY, null)) },
                onGarage = { navController.navigate(Routes.GARAGE) },
                onMissions = { navController.navigate(Routes.MISSIONS) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.CAREER) {
            LevelMapScreen(
                progress = progress,
                selectedBoosters = selectedBoosters,
                adsConsentResolved = adsConsentResolved,
                onToggleBooster = viewModel::toggleBooster,
                onPlayLevel = { levelId ->
                    navController.navigate(Routes.game(RunMode.CAREER, levelId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GARAGE) {
            // Odullu "bedava coin" reklami: odul SADECE SDK gercekten
            // "kazanildi" derse verilir, reklam yuklenemezse ekran normal calisir.
            var coinAdInFlight by remember { mutableStateOf(false) }
            // Reklam yuklenemezse ekran SUSMUYOR (docs/REVIEW_UX.md §4).
            // Eskiden `onFailure` bostu: buton bir an "…" olup normale
            // donuyor, coin gelmiyor ve hicbir aciklama cikmiyordu.
            var coinAdFailed by remember { mutableStateOf(false) }
            GarageScreen(
                progress = progress,
                adsConsentResolved = adsConsentResolved,
                adInFlight = coinAdInFlight,
                adFailed = coinAdFailed,
                onWatchAdForCoins = {
                    // Onay kapisi burada da gecerli (2026-08-19): garaj
                    // odullu reklami da onaya bakmadan yukleniyordu.
                    //
                    // ⚠ EKSIK KALAN: onay yokken "İZLE" butonu hala
                    // gorunuyor ve basilinca "Reklam yüklenemedi" diyor.
                    // Dogrusu butonun HIC cikmamasi — bunun icin
                    // `GarageScreen`e bir `rewardedOfferAllowed` parametresi
                    // gerekiyor ve o dosya bu gorevin kapsami disinda
                    // (paralel ajan calisiyor). Politika acigi kapali:
                    // onaysiz reklam ISTEGI cikmiyor.
                    val offerAllowed = AdConsentGate.shouldOfferRewarded(
                        adsAllowed = AdConsentGate.adsAllowed(
                            consentLatched = adsConsentResolved,
                            sdkCanRequestAds = ConsentManager.canRequestAds(context)
                        ),
                        activityAvailable = activity != null
                    )
                    if (!offerAllowed) {
                        // Sessiz kalmiyoruz: buton bir sey yapmiyor gibi
                        // gorunmesin (docs/REVIEW_UX.md §4).
                        coinAdFailed = true
                    } else if (activity != null && !coinAdInFlight) {
                        coinAdInFlight = true
                        coinAdFailed = false
                        RewardedAdManager.loadAndShow(
                            context = activity,
                            activity = activity,
                            onRewardEarned = { viewModel.grantRewardedCoins() },
                            onFailure = { coinAdFailed = true },
                            onAdClosed = { coinAdInFlight = false }
                        )
                    }
                },
                onUpgrade = viewModel::upgrade,
                onBuyBooster = viewModel::buyBooster,
                onSelectCarShape = viewModel::selectCarShape,
                onBuyCarShape = viewModel::buyCarShape,
                onSelectCarColor = viewModel::selectCarColor,
                onBuyCarColor = viewModel::buyCarColor,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MISSIONS) {
            MissionsScreen(
                progress = progress,
                missions = missions,
                daily = daily,
                adsConsentResolved = adsConsentResolved,
                onClaimTier = viewModel::claimMissionTier,
                onClaimChest = viewModel::claimWeeklyChest,
                onPlayDaily = { navController.navigate(Routes.game(RunMode.DAILY, null)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                progress = progress,
                adsConsentResolved = adsConsentResolved,
                onSoundEnabled = viewModel::setSoundEnabled,
                onVibrationEnabled = viewModel::setVibrationEnabled,
                onLanguage = viewModel::setLanguage,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.GAME,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("level") { type = NavType.IntType },
                navArgument("training") { type = NavType.BoolType }
            )
        ) { entry ->
            val mode = runCatching {
                RunMode.valueOf(entry.arguments?.getString("mode") ?: RunMode.ENDLESS.name)
            }.getOrDefault(RunMode.ENDLESS)
            val levelArg = entry.arguments?.getInt("level") ?: -1
            val training = entry.arguments?.getBoolean("training") ?: false

            GameScreen(
                mode = mode,
                levelId = levelArg.takeIf { it > 0 },
                training = training,
                viewModel = viewModel,
                // Oyun ekraninda BANNER YOK ama gecis ve odullu reklam VAR.
                // Bayrak buraya 2026-08-19'a kadar hic verilmiyordu: diger
                // bes ekran onu yalnizca banner icin aliyordu, oyun ekrani
                // ise reklamlarini onaya hic bakmadan gosteriyordu.
                adsConsentResolved = adsConsentResolved,
                onExit = { navController.popBackStack() },
                onPlayLevel = { nextLevel ->
                    // Sonraki bolum, mevcut oyun ekraninin YERINE gelir.
                    navController.navigate(Routes.game(RunMode.CAREER, nextLevel)) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                },
                onRetry = {
                    // Ayni mod bastan baslar ve mevcut oyun ekraninin YERINE
                    // gelir — geri yiginina ust uste kosu birikmesin.
                    navController.navigate(Routes.game(mode, levelArg.takeIf { it > 0 })) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                }
            )
        }
    }
}
