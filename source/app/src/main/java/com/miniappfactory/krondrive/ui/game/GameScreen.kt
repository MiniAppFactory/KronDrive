package com.miniappfactory.krondrive.ui.game

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniappfactory.krondrive.ads.InterstitialAdManager
import com.miniappfactory.krondrive.ads.RewardedAdManager
import com.miniappfactory.krondrive.audio.EngineSoundManager
import com.miniappfactory.krondrive.data.AppLanguage
import com.miniappfactory.krondrive.game.GameConfig
import com.miniappfactory.krondrive.game.GameEvent
import com.miniappfactory.krondrive.game.LevelCatalog
import com.miniappfactory.krondrive.game.LevelGoal
import com.miniappfactory.krondrive.game.RunMode
import com.miniappfactory.krondrive.game.RunPhase
import com.miniappfactory.krondrive.game.RunResult
import com.miniappfactory.krondrive.ui.KronViewModel
import com.miniappfactory.krondrive.ui.common.KronCard
import com.miniappfactory.krondrive.ui.common.KronProgressBar
import com.miniappfactory.krondrive.ui.common.PrimaryButton
import com.miniappfactory.krondrive.ui.common.SecondaryButton
import com.miniappfactory.krondrive.ui.common.ObjectiveDots
import com.miniappfactory.krondrive.ui.theme.KronColors

/** HUD'daki tek bir hedef satiri: "GEÇİŞ 4/10" + durumu. */
private data class HudObjective(
    val label: String,
    /** "4/10" bicimi; sayilamayan hedeflerde bos. */
    val value: String,
    val met: Boolean
)

/** HUD'un okudugu, kare kare degil de seyrek guncellenen ozet. */
private data class HudState(
    val score: Int = 0,
    val timeLabel: String = "",
    val boost: Float = GameConfig.BOOST_MAX,
    val combo: Int = 0,
    /**
     * Bolumun TUM hedefleri (kariyerde uc yildiz, gunluk gorevde uc kademe).
     * Sonsuz modda bos. Oyuncu neyin eksik oldugunu ekranda gormeli.
     */
    val objectives: List<HudObjective> = emptyList(),
    /** Hiz kilidi butonu gosterilsin mi (yalnizca sonsuz mod). */
    val speedLockAvailable: Boolean = false,
    val speedLocked: Boolean = false,
    /** Boost su an tutusabilir mi (bos/kilitliyse buton soluklasir). */
    val boostReady: Boolean = true
)

/**
 * Oyun ekrani: motoru surer, sahneyi cizer, kontrolleri ve tum kosu-ici
 * overlay'leri (geri sayim, duraklatma, carpisma, sonuc) yonetir.
 *
 * Banner reklam BU EKRANDA YOKTUR — kontrollerin ustune reklam gelmemeli.
 * Gecis reklami sadece sonuc ekranindan cikarken, odullu reklam sadece
 * "carpisma sonrasi devam" icin kullanilir.
 */
@Composable
fun GameScreen(
    mode: RunMode,
    levelId: Int?,
    viewModel: KronViewModel,
    onExit: () -> Unit,
    onPlayLevel: (Int) -> Unit
) {
    val progress by viewModel.playerProgress.collectAsStateWithLifecycle()
    val daily by viewModel.dailyChallenge.collectAsStateWithLifecycle()
    val dailyReward by viewModel.lastDailyReward.collectAsStateWithLifecycle()
    val language = progress.language
    val context = LocalContext.current
    val activity = context as? Activity
    val density = LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()
    val haptics = LocalHapticFeedback.current

    // Yeniden dene: anahtari degistirmek yeni bir motor (yeni kosu) yaratir.
    var runKey by remember { mutableIntStateOf(0) }
    val engine = remember(runKey, mode, levelId) { viewModel.createEngine(mode, levelId) }

    var frame by remember(engine) { mutableIntStateOf(0) }
    // Motorun `phase` alani Compose durumu DEGIL — overlay'lerin dogru anda
    // gorunmesi icin her karede snapshot durumuna yansitiliyor.
    var phase by remember(engine) { mutableStateOf(engine.phase) }
    var hud by remember(engine) { mutableStateOf(HudState()) }
    var dodgeBanner by remember(engine) { mutableStateOf<String?>(null) }
    var dodgeBannerUntil by remember(engine) { mutableStateOf(0L) }
    var result by remember(engine) { mutableStateOf<RunResult?>(null) }
    var showCrashDialog by remember(engine) { mutableStateOf(false) }
    var adInFlight by remember(engine) { mutableStateOf(false) }
    var adFailed by remember(engine) { mutableStateOf(false) }
    var paused by remember(engine) { mutableStateOf(false) }
    var coinsDoubled by remember(engine) { mutableStateOf(false) }
    /** Reklamdan gercekten odenen bonus coin ve "gunluk sinir doldu" durumu. */
    var bonusCoins by remember(engine) { mutableIntStateOf(0) }
    var bonusLimitReached by remember(engine) { mutableStateOf(false) }

    val level = engine.level

    fun publishResult(runResult: RunResult) {
        if (result != null) return
        result = runResult
        showCrashDialog = false
        EngineSoundManager.idle()
        viewModel.onRunFinished(runResult)
    }

    // Sesi ekran omru boyunca ac/kapat; ayarlarda ses kapaliysa hic baslatma.
    // Motor ve korna sesi SECILI GOVDEYE gore degisir (sahibi istegi,
    // 2026-08-15); profil tablosu `audio/CarSoundProfile.kt` icinde, gövde
    // id'siyle eslesir ve bilinmeyen id varsayilana duser.
    DisposableEffect(progress.soundEnabled, engine) {
        EngineSoundManager.setProfile(engine.carStyle.shape.id)
        EngineSoundManager.setEnabled(progress.soundEnabled)
        if (progress.soundEnabled) EngineSoundManager.start()
        onDispose { EngineSoundManager.stop() }
    }

    // Uygulama arka plana alinirsa oyun DURUR ve oyuncu geri donunce kendi
    // istegiyle devam eder — telefonu cebe koyup geri donunce carpmis olmasin.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, engine) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (engine.phase == RunPhase.RUNNING || engine.phase == RunPhase.COUNTDOWN) {
                        engine.pause()
                        paused = true
                    }
                    EngineSoundManager.stop()
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (progress.soundEnabled) EngineSoundManager.start()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ana oyun dongusu: her ekran karesinde bir simulasyon adimi.
    LaunchedEffect(engine) {
        var lastFrameNanos = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                val dt = if (lastFrameNanos == 0L) 0f else (now - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = now

                val events = engine.step(dt)
                for (event in events) {
                    when (event) {
                        is GameEvent.PerfectDodge -> {
                            dodgeBanner = if (event.combo > 1) {
                                "PERFECT DODGE ×${event.combo}"
                            } else {
                                "PERFECT DODGE"
                            }
                            dodgeBannerUntil = now + 900_000_000L
                        }

                        is GameEvent.BoostStarted -> EngineSoundManager.playNitro()

                        is GameEvent.Crash -> {
                            if (!event.saved) showCrashDialog = true
                        }

                        is GameEvent.Finished -> publishResult(event.result)
                        else -> Unit
                    }
                }

                if (engine.phase == RunPhase.RUNNING) {
                    EngineSoundManager.update(engine.speed, engine.boosting)
                } else {
                    EngineSoundManager.idle()
                }

                if (dodgeBanner != null && now > dodgeBannerUntil) dodgeBanner = null
                if (engine.phase != phase) phase = engine.phase

                // HUD her karede degil, ~her 3 karede bir guncellenir — metin
                // yeniden bestelemesi 60 Hz'de gereksiz maliyet.
                if (frame % 3 == 0) {
                    hud = buildHud(engine, language)
                }
                frame++
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(KronColors.Background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Motor "dp uzayinda" calisir; viewport'u da dp olarak veriyoruz.
            engine.setViewport(size.width / density, size.height / density)
            @Suppress("UNUSED_EXPRESSION")
            frame // her karede yeniden cizim icin okunuyor
            drawGameScene(
                engine = engine,
                density = density,
                textMeasurer = textMeasurer,
                // Gosterge kucultuldu (sahibi geri bildirimi, 2026-08-15:
                // "hiz gostergesi cok buyuk ve cok ortada"). Rakam yolun
                // uzerine tasmayacak kadar kucuk, yine de tek bakista okunur.
                gaugeValueSize = 24.sp,
                gaugeLabelSize = 8.sp,
                gaugeSmallSize = 9.sp
            )
        }

        dodgeBanner?.let { text ->
            Text(
                text = text,
                color = KronColors.AccentBright,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 120.dp)
            )
        }

        if (result == null && !showCrashDialog) {
            // Kaydirarak serit degistirme. Butonlarin ALTINDA duruyor, yani
            // butona basmak her zaman oncelikli; ekranin geri kalaninda parmagi
            // saga/sola surukleyerek de serit degistirilebiliyor. Oyuncu
            // "tus hassasiyeti iyi degil" dedigi icin eklendi (2026-08-13).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(engine) {
                        val threshold = 36.dp.toPx()
                        var accumulated = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accumulated = 0f },
                            onDragEnd = { accumulated = 0f },
                            onDragCancel = { accumulated = 0f }
                        ) { _, dragAmount ->
                            accumulated += dragAmount
                            if (accumulated >= threshold) {
                                engine.steerRight()
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                accumulated = 0f
                            } else if (accumulated <= -threshold) {
                                engine.steerLeft()
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                accumulated = 0f
                            }
                        }
                    }
            )
            DrivingControls(
                engine = engine,
                language = language,
                boostReady = hud.boostReady,
                // Korna oynanisi HIC etkilemez; ses kapaliyken buton hic
                // gosterilmez (olu butona basmak "bozuk" hissi verir).
                hornAvailable = progress.soundEnabled,
                onHorn = {
                    // Bekleme suresi dolmadiysa ses calmaz; titresim de
                    // ancak ses gercekten caldiysa verilir.
                    if (EngineSoundManager.playHorn()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                onSteer = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }

        // HUD, kaydirma katmaninin ve kontrollerin USTUNDE cizilir.
        // Eskiden altta kaliyordu: tam ekran kaydirma katmani (serit
        // degistirme) dokunusu yutuyor ve DURAKLAT TUSU CALISMIYORDU
        // (cihazda dogrulandi, 2026-08-15). Kontrol butonlari calisiyordu
        // cunku onlar zaten kaydirma katmanindan SONRA ciziliyordu.
        GameHud(
            hud = hud,
            onPause = {
                if (engine.phase == RunPhase.RUNNING) {
                    engine.pause()
                    paused = true
                }
            },
            onToggleSpeedLock = {
                engine.toggleSpeedLock()
                // HUD ~3 karede bir guncelleniyor; kilit durumu ANINDA
                // gorunsun diye burada da yansitiliyor.
                hud = hud.copy(speedLocked = engine.speedLocked)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        )


        if (phase == RunPhase.COUNTDOWN) {
            CountdownOverlay(engine.countdownRemaining, language)
        }

        if (paused && result == null) {
            PausedOverlay(
                language = language,
                onResume = {
                    engine.resume()
                    paused = false
                },
                onQuit = {
                    engine.finish(completed = false)
                    engine.lastResult?.let { publishResult(it) }
                    // Duraklatmadan cikmak da bir kosu sonudur: reklamsiz
                    // cikis yolu birakilmiyor (sahibi karari, 2026-08-14).
                    withOptionalInterstitial(viewModel, mode, activity) { onExit() }
                }
            )
        }

        if (showCrashDialog && result == null) {
            CrashOverlay(
                language = language,
                canRevive = engine.canRevive(),
                adInFlight = adInFlight,
                adFailed = adFailed,
                onWatchAd = {
                    if (activity != null && !adInFlight) {
                        adInFlight = true
                        adFailed = false
                        RewardedAdManager.loadAndShow(
                            context = context,
                            activity = activity,
                            onRewardEarned = {
                                engine.revive()
                                showCrashDialog = false
                            },
                            onFailure = { adFailed = true },
                            onAdClosed = { adInFlight = false }
                        )
                    }
                },
                onGiveUp = {
                    engine.finish(completed = false)
                    engine.lastResult?.let { publishResult(it) }
                }
            )
        }

        result?.let { runResult ->
            val nextLevelId = runResult.levelId?.plus(1)
            // "Sonraki bolum" ancak bolum GERCEKTEN gecildiyse cikar; aksi
            // halde buton oyuncuyu kilitli bir bolume goturuyordu.
            val hasNext = mode == RunMode.CAREER &&
                nextLevelId != null &&
                nextLevelId <= LevelCatalog.count &&
                runResult.passed

            RunResultOverlay(
                result = runResult,
                language = language,
                // Her hedef icin etiket + "1280/1400" bicimi ilerleme
                // (sahibi istegi, 2026-08-15): oyuncu neyi ne kadar
                // kacirdigini gormeli, yalnizca "olmadi" degil.
                objectiveLabels = level?.stars?.map { objective ->
                    objective.label(language) to
                        objective.progressText(runResult.stats).orEmpty()
                }.orEmpty(),
                tierRewards = if (mode == RunMode.DAILY) {
                    daily.challenge.tiers.map { it.rewardCoins }
                } else {
                    emptyList()
                },
                dailyReward = dailyReward,
                hasNextLevel = hasNext,
                coinsDoubled = coinsDoubled,
                bonusCoins = bonusCoins,
                bonusLimitReached = bonusLimitReached,
                adInFlight = adInFlight,
                onDoubleCoins = {
                    if (activity != null && !adInFlight) {
                        adInFlight = true
                        RewardedAdManager.loadAndShow(
                            context = context,
                            activity = activity,
                            onRewardEarned = {
                                // Odul SADECE SDK "kazanildi" derse verilir.
                                // Gunluk sinir repository tarafinda; dolmussa
                                // 0 doner ve ekran bunu soyler (sessizce
                                // "ikiye katlandi" yazip coin vermemek olmaz).
                                viewModel.grantBonusCoins(runResult.coinsEarned) { granted ->
                                    bonusCoins = granted
                                    coinsDoubled = granted > 0
                                    bonusLimitReached = granted == 0
                                }
                            },
                            onFailure = { },
                            onAdClosed = { adInFlight = false }
                        )
                    }
                },
                onNext = {
                    withOptionalInterstitial(viewModel, mode, activity) {
                        nextLevelId?.let(onPlayLevel)
                    }
                },
                onHome = {
                    withOptionalInterstitial(viewModel, mode, activity) { onExit() }
                }
            )
        }
    }
}

/**
 * Kosudan cikarken gecis reklami. [GameConfig.INTERSTITIAL_AFTER_EVERY_RUN]
 * acikken her cikista gosterilir; kapaliysa eski N-sayac esigine duser.
 * Reklam yuklenemezse akis beklemeden devam eder — reklam oyunu asla bloklamaz.
 */
private fun withOptionalInterstitial(
    viewModel: KronViewModel,
    mode: RunMode,
    activity: Activity?,
    proceed: () -> Unit
) {
    val shouldShow = GameConfig.INTERSTITIAL_AFTER_EVERY_RUN ||
        viewModel.shouldShowInterstitial(mode)
    if (activity != null && shouldShow) {
        viewModel.onInterstitialShown(mode)
        InterstitialAdManager.loadAndShow(activity, activity) { proceed() }
    } else {
        proceed()
    }
}

private fun buildHud(
    engine: com.miniappfactory.krondrive.game.GameEngine,
    language: AppLanguage
): HudState {
    val level = engine.level
    val timeLabel = when (val goal = level?.goal) {
        is LevelGoal.SurviveTime -> formatTime(engine.timeRemaining?.toInt() ?: 0)
        is LevelGoal.ReachDistance -> "${engine.distanceMeters()} / ${goal.meters} m"
        null -> formatTime(engine.timeElapsed.toInt())
    }
    val stats = engine.currentStats()
    // Tum hedefler listelenir (kariyerde uc yildiz, gunlukte uc kademe).
    // Tam cumle yerine kisa etiket: HUD ekranin ustunu kapatmasin.
    val objectives = level?.stars.orEmpty().map { objective ->
        HudObjective(
            label = objective.shortLabel(language),
            value = objective.progressText(stats).orEmpty(),
            met = objective.isMet(stats)
        )
    }
    return HudState(
        score = stats.score,
        timeLabel = timeLabel,
        boost = engine.boost,
        combo = engine.combo,
        objectives = objectives,
        speedLockAvailable = engine.canLockSpeed(),
        speedLocked = engine.speedLocked,
        boostReady = engine.isBoostReady()
    )
}

/**
 * Tek hedef satiri: ici bos yuvarlak + kisa etiket + "4/10".
 * Hedef saglaninca yuvarlak yesile doner ve icine tik gelir — oyuncu
 * ilerledigini aninda gorur.
 */
@Composable
private fun ObjectiveRow(row: HudObjective, style: TextStyle) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(top = 3.dp)
    ) {
        Text(
            text = if (row.value.isEmpty()) row.label else "${row.label} ${row.value}",
            style = style,
            color = if (row.met) OBJECTIVE_DONE else KronColors.Accent,
            fontSize = 11.sp
        )
        Box(
            modifier = Modifier.size(13.dp),
            contentAlignment = Alignment.Center
        ) {
            if (row.met) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .background(OBJECTIVE_DONE, CircleShape)
                )
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = KronColors.Background,
                    modifier = Modifier.size(10.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .border(1.5.dp, KronColors.Accent, CircleShape)
                )
            }
        }
    }
}

/** Tamamlanan hedefin rengi — yolun her temasinda okunacak kadar parlak. */
private val OBJECTIVE_DONE = Color(0xFF3DDC84)

private fun formatTime(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

// ---------------------------------------------------------------------------
// HUD
// ---------------------------------------------------------------------------

@Composable
private fun GameHud(
    hud: HudState,
    onPause: () -> Unit,
    onToggleSpeedLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Panel YOK: gostergeler dogrudan yolun uzerinde, sadece golgeyle okunakli.
    // Oyuncu geri bildirimi (2026-08-13): ustteki koyu kutu hizlandikca yolu
    // goremeyecek kadar cok yer kapliyordu. Simdi skor sol ust, sure/hedef sag
    // ust kosede; boost ise ekranin en ustunde ince bir serit.
    val hudText = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        shadow = Shadow(Color(0xCC000000), Offset(0f, 2f), 4f)
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0x40000000))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((hud.boost / GameConfig.BOOST_MAX).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(KronColors.BlueBright)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
        ) {
            Text(
                text = "${hud.score}",
                style = hudText,
                color = KronColors.TextPrimary,
                fontSize = 20.sp
            )
            if (hud.combo > 1) {
                Text(
                    text = "COMBO ×${hud.combo}",
                    style = hudText,
                    color = KronColors.AccentBright,
                    fontSize = 13.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = hud.timeLabel,
                style = hudText,
                color = KronColors.TextPrimary,
                fontSize = 18.sp
            )
            // Bolumu gecmek icin gereken hedefler, YOLUN DISINDA bir kontrol
            // listesi olarak (sahibi istegi, 2026-08-15): "oyuncular ne
            // yapmalari gerektigini ezberlemek zorunda kalmasin". Ici bos
            // yuvarlak = henuz yapilmadi, yesil tik = tamam.
            if (hud.objectives.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                hud.objectives.forEach { row ->
                    ObjectiveRow(row = row, style = hudText)
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0x66040C16), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) { detectTapGestures(onPress = { onPause() }) },
                contentAlignment = Alignment.Center
            ) {
                Text("II", color = KronColors.Accent, fontSize = 14.sp)
            }

            // Hiz kilidi YALNIZCA sonsuz modda (sahibi istegi, 2026-08-15):
            // "uzun sure gitmek isteyen varsa sabitlesin, yavas yavas gitsin".
            // Kariyer ve gunluk gorevde hedefler sureye/hiza bagli oldugu icin
            // boyle bir dugme dengeyi bozardi, o yuzden hic gosterilmiyor.
            if (hud.speedLockAvailable) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .background(
                            if (hud.speedLocked) KronColors.Accent.copy(alpha = 0.85f)
                            else Color(0x66040C16),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = { onToggleSpeedLock() })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hud.speedLocked) "🔒 HIZ" else "HIZ",
                        color = if (hud.speedLocked) KronColors.Background else KronColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Kontroller (prototipteki son yerlesim: yonler ustte, fren/boost altta)
// ---------------------------------------------------------------------------

/**
 * Kontrol olculeri tek yerde. Oyuncu geri bildirimi (2026-08-14): "hiz artinca
 * frene bastiktan sonra sola yon vermek icin uzak kaliyor" — butonlar
 * kuculdu (88 -> 76 dp) ve ayni taraftaki iki buton dikeyde yakinlasti
 * (aradaki bosluk 30 -> 12 dp). Bir parmak fren/boost ile yon butonu arasinda
 * kisa bir kaydirmayla gidip gelebiliyor.
 */
private val CONTROL_SIZE = 64.dp
private val CONTROL_EDGE_PADDING = 16.dp
private val CONTROL_BOTTOM_PADDING = 24.dp

/**
 * Ayni taraftaki iki buton arasindaki dikey bosluk.
 * 30 -> 12 -> 8 -> 5 dp (uc tur oyuncu geri bildirimi, 2026-08-14/15).
 * Butonlar 64 dp'ye kuculdugu icin 5 dp'de bile iki dokunma hedefi arasinda
 * ~1.5 mm bosluk kaliyor; sifirlamak yanlis butona basma riskini gercekten
 * artirirdi.
 */
private val CONTROL_VERTICAL_GAP = 5.dp

/**
 * Buton govdeleri OPAK ve kabartmali (sahibi: "silik olmasin, buton gibi
 * kabartmali olsun"). Once %10 beyaz saydam kutulardi ve yol uzerinde
 * kayboluyorlardi.
 *
 * Kabartma uc katmanla yapiliyor: (1) dikey gradyan — ust acik, alt koyu,
 * (2) ust kenarda ince acik cizgi (isik), (3) altta golge (elevation).
 * Basiliyken gradyan TERSINE doner ve golge kaybolur: parmak butonu iceri
 * itmis gibi gorunur.
 */
private val CONTROL_SHAPE = CircleShape
private val CONTROL_ELEVATION = 5.dp
private val CONTROL_ELEVATION_PRESSED = 1.dp

private val STEER_TOP = Color(0xFF32496C)
private val STEER_BOTTOM = Color(0xFF0E1A2E)
private val BRAKE_TOP = Color(0xFFB03A46)
private val BRAKE_BOTTOM = Color(0xFF48111A)
private val BOOST_TOP = Color(0xFF2189D6)
private val BOOST_BOTTOM = Color(0xFF08375F)

/** Boost bos/kilitliyken: renk kacar, buton "simdi olmaz" der. */
private val BOOST_EMPTY_TOP = Color(0xFF2C3B4F)
private val BOOST_EMPTY_BOTTOM = Color(0xFF141D2B)

/**
 * Korna (2026-08-15, sahibi istegi: *"bir tane de korna efekti koyalim, ise
 * yaramasa da eglencelik olur"*). Oynanisa etkisi YOKTUR.
 *
 * **Yeri: alt orta.** Uc secenek vardi ve ikisi elendi:
 *  - Ustteki HUD satiri (duraklat + hiz kilidi): oyuncunun iki basparmagi
 *    alt koselerde duruyor, surerken ekranin tepesine uzanmak gerekirdi.
 *  - Kontrol kumelerinin yanina: yon/fren/boost butonlari arasindaki bosluk
 *    2026-08-14/15'te uc kez oyuncu geri bildirimiyle daraltildi
 *    ([CONTROL_VERTICAL_GAP] 30 -> 5 dp); araya bir buton daha sokmak o
 *    calismayi geri alirdi.
 *
 * Alt ortada 64 dp'lik kumelerin arasinda genis bir bosluk zaten var: 360 dp
 * genisliginde fren 16–80, boost 280–344 dp'de; 48 dp'lik korna ~156–204'e
 * oturuyor ve iki yanda ~76 dp bosluk kaliyor. Butonun kendisi de bilerek
 * KUCUK ve sonuk: bir kontrol degil, bir suslemedir. Kontrollerle ayni
 * katmanda oldugu icin ustundeki kaydirma (serit degistirme) katmanini da
 * bozmaz ve sonuc/carpisma ekranlarinda kontrollerle birlikte kaybolur.
 */
private val HORN_SIZE = 48.dp
private val HORN_TOP = Color(0xFF6E5B2C)
private val HORN_BOTTOM = Color(0xFF251F10)

/** Korna, fren/boost ile dikeyde ayni hizada dursun diye. */
private val HORN_BOTTOM_PADDING =
    CONTROL_BOTTOM_PADDING + (CONTROL_SIZE - HORN_SIZE) / 2f

/** Ust kenardaki isik cizgisi ve dis cerceve. */
private val CONTROL_HIGHLIGHT = Color(0x59FFFFFF)
private val CONTROL_BORDER = Color(0x40FFFFFF)

/** Yon butonu, alttaki fren/boost butonunun tam ustunde durur. */
private val STEER_BOTTOM_PADDING =
    CONTROL_BOTTOM_PADDING + CONTROL_SIZE + CONTROL_VERTICAL_GAP

@Composable
private fun DrivingControls(
    engine: com.miniappfactory.krondrive.game.GameEngine,
    language: AppLanguage,
    boostReady: Boolean,
    hornAvailable: Boolean,
    onHorn: () -> Unit,
    onSteer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ControlButton(
            label = "◀",
            topColor = STEER_TOP,
            bottomColor = STEER_BOTTOM,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = CONTROL_EDGE_PADDING, bottom = STEER_BOTTOM_PADDING)
                .size(CONTROL_SIZE),
            onPress = {
                engine.steerLeft()
                onSteer()
            }
        )
        ControlButton(
            label = "▶",
            topColor = STEER_TOP,
            bottomColor = STEER_BOTTOM,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = CONTROL_EDGE_PADDING, bottom = STEER_BOTTOM_PADDING)
                .size(CONTROL_SIZE),
            onPress = {
                engine.steerRight()
                onSteer()
            }
        )
        HoldButton(
            label = language.pick(tr = "FREN", en = "BRAKE"),
            topColor = BRAKE_TOP,
            bottomColor = BRAKE_BOTTOM,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = CONTROL_EDGE_PADDING, bottom = CONTROL_BOTTOM_PADDING)
                .size(CONTROL_SIZE),
            onHoldChanged = { engine.setBrake(it) }
        )
        HoldButton(
            label = "⚡",
            // Boost bosaldiginda ve parmak basiliyken KILITLENIR (bkz.
            // GameConfig.BOOST_REENGAGE_MIN). Buton eskiden bunu hic belli
            // etmiyordu: oyuncu basiyor, hicbir sey olmuyor ve "boost yaptim
            // ama saymadi" diyordu (2026-08-15). Artik hazir degilken soluk.
            topColor = if (boostReady) BOOST_TOP else BOOST_EMPTY_TOP,
            bottomColor = if (boostReady) BOOST_BOTTOM else BOOST_EMPTY_BOTTOM,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = CONTROL_EDGE_PADDING, bottom = CONTROL_BOTTOM_PADDING)
                .size(CONTROL_SIZE),
            onHoldChanged = { engine.setBoost(it) }
        )
        if (hornAvailable) {
            ControlButton(
                label = "📣",
                topColor = HORN_TOP,
                bottomColor = HORN_BOTTOM,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = HORN_BOTTOM_PADDING)
                    .size(HORN_SIZE),
                labelSize = 18.sp,
                onPress = onHorn
            )
        }
    }
}

/**
 * Kabartma efektinin ortak govdesi. [pressed] true iken gradyan ters doner
 * ve golge kaybolur — buton iceri itilmis gibi durur.
 */
@Composable
private fun RaisedControl(
    topColor: Color,
    bottomColor: Color,
    pressed: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (pressed) CONTROL_ELEVATION_PRESSED else CONTROL_ELEVATION,
        label = "controlElevation"
    )
    Box(
        modifier = modifier
            .shadow(elevation, CONTROL_SHAPE)
            .background(
                brush = Brush.verticalGradient(
                    if (pressed) listOf(bottomColor, topColor) else listOf(topColor, bottomColor)
                ),
                shape = CONTROL_SHAPE
            )
            .border(1.dp, CONTROL_BORDER, CONTROL_SHAPE),
        contentAlignment = Alignment.Center
    ) {
        // Ust yaridaki parlaklik: dairede duz bir cizgi yamuk duruyordu,
        // onun yerine yukaridan asagi sonumlenen bir cam parlamasi.
        if (!pressed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .clip(CONTROL_SHAPE)
                    .background(
                        Brush.verticalGradient(
                            listOf(CONTROL_HIGHLIGHT, Color.Transparent)
                        )
                    )
            )
        }
        content()
    }
}

@Composable
private fun ControlButton(
    label: String,
    topColor: Color,
    bottomColor: Color,
    modifier: Modifier,
    labelSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    onPress: () -> Unit
) {
    // Yon butonu bir DOKUNUS butonu; basili kalma suresi oyunu etkilemez ama
    // parmak degdiginde gorsel olarak da cokmesi gerekiyor.
    var pressed by remember { mutableStateOf(false) }
    RaisedControl(
        topColor = topColor,
        bottomColor = bottomColor,
        pressed = pressed,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    onPress()
                    tryAwaitRelease()
                    pressed = false
                }
            )
        }
    ) {
        Text(label, color = Color.White, fontSize = labelSize, fontWeight = FontWeight.Black)
    }
}

/** Basili tutuldugu surece aktif olan buton (boost/fren). */
@Composable
private fun HoldButton(
    label: String,
    topColor: Color,
    bottomColor: Color,
    modifier: Modifier,
    onHoldChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    RaisedControl(
        topColor = topColor,
        bottomColor = bottomColor,
        pressed = pressed,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    onHoldChanged(true)
                    // Parmak kalkana (veya hareket iptal olana) kadar basili sayilir.
                    tryAwaitRelease()
                    pressed = false
                    onHoldChanged(false)
                }
            )
        }
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

// ---------------------------------------------------------------------------
// Overlay'ler
// ---------------------------------------------------------------------------

@Composable
private fun OverlayScrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3010610))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun CountdownOverlay(remaining: Float, language: AppLanguage) {
    OverlayScrim {
        KronCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    language.pick(tr = "HAZIR OL", en = "GET READY"),
                    color = KronColors.Accent,
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val seconds = kotlin.math.ceil(remaining).toInt()
                Text(
                    text = if (seconds <= 0) "GO!" else "$seconds",
                    color = KronColors.BlueBright,
                    fontSize = 52.sp
                )
            }
        }
    }
}

@Composable
private fun PausedOverlay(
    language: AppLanguage,
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    OverlayScrim {
        KronCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    language.pick(tr = "DURAKLATILDI", en = "PAUSED"),
                    color = KronColors.Accent,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                PrimaryButton(
                    text = language.pick(tr = "DEVAM ET", en = "RESUME"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onResume
                )

                Spacer(modifier = Modifier.height(10.dp))
                SecondaryButton(
                    text = language.pick(tr = "ÇIKIŞ", en = "QUIT"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onQuit
                )
            }
        }
    }
}

@Composable
private fun CrashOverlay(
    language: AppLanguage,
    canRevive: Boolean,
    adInFlight: Boolean,
    adFailed: Boolean,
    onWatchAd: () -> Unit,
    onGiveUp: () -> Unit
) {
    OverlayScrim {
        KronCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    language.pick(tr = "ÇARPTIN!", en = "CRASHED!"),
                    color = KronColors.Danger,
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (canRevive) {
                    Text(
                        language.pick(
                            tr = "Reklam izleyip kaldığın yerden devam edebilirsin.",
                            en = "Watch an ad to continue from where you crashed."
                        ),
                        color = KronColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PrimaryButton(
                        text = if (adInFlight) {
                            language.pick(tr = "YÜKLENİYOR…", en = "LOADING…")
                        } else {
                            language.pick(tr = "REKLAM İZLE → DEVAM", en = "WATCH AD → CONTINUE")
                        },
                        enabled = !adInFlight,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onWatchAd
                    )
                    if (adFailed) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            language.pick(
                                tr = "Reklam yüklenemedi. İnternet bağlantını kontrol et.",
                                en = "Ad could not be loaded. Check your connection."
                            ),
                            color = KronColors.TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                SecondaryButton(
                    text = language.pick(tr = "SONUÇLARI GÖR", en = "SEE RESULTS"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onGiveUp
                )
            }
        }
    }
}

@Composable
private fun RunResultOverlay(
    result: RunResult,
    language: AppLanguage,
    /** (hedef metni, "1280/1400" ilerleme metni) ciftleri. */
    objectiveLabels: List<Pair<String, String>>,
    /** Gunluk gorevde her kademenin coin odulu; kariyerde bos. */
    tierRewards: List<Int>,
    /** Bu kosuda gunluk gorevden GERCEKTEN odenen coin (0 = yeni kademe yok). */
    dailyReward: Int,
    hasNextLevel: Boolean,
    coinsDoubled: Boolean,
    /** Reklamdan gercekten odenen bonus coin (sinir yuzunden kirpilmis olabilir). */
    bonusCoins: Int,
    /** Odullu reklamin gunluk siniri dolduysa buton yerine aciklama gosterilir. */
    bonusLimitReached: Boolean,
    adInFlight: Boolean,
    onDoubleCoins: () -> Unit,
    onNext: () -> Unit,
    onHome: () -> Unit
) {
    val stats = result.stats
    OverlayScrim {
        KronCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Kariyerde "TAMAMLANDI" yalnizca TUM gorevler bittiginde
                // yazar. Sureyi doldurmak ama gorevleri yapmamak bolumu
                // gecirmiyor (bkz. RunResult.passed) — baslik da bunu
                // durustce soylemeli, aksi halde oyuncu bolumun acildigini
                // sanip haritada kilitli buluyordu.
                val careerFailed = result.mode == RunMode.CAREER && !result.passed
                Text(
                    text = when {
                        result.mode == RunMode.ENDLESS && result.newRecord ->
                            language.pick(tr = "YENİ REKOR!", en = "NEW RECORD!")

                        careerFailed -> language.pick(tr = "GÖREVLER EKSİK", en = "OBJECTIVES LEFT")
                        stats.completed -> language.pick(tr = "TAMAMLANDI", en = "COMPLETED")
                        else -> language.pick(tr = "KOŞU BİTTİ", en = "RUN OVER")
                    },
                    color = if (careerFailed) KronColors.TextSecondary else KronColors.Accent,
                    fontSize = 28.sp
                )
                if (careerFailed) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = language.pick(
                            tr = "Bölümü geçmek için üç görevin üçü de gerekli.",
                            en = "All three objectives are required to pass."
                        ),
                        color = KronColors.TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (result.mode == RunMode.CAREER) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ObjectiveDots(earned = result.stars, dotSize = 30)
                }

                result.secondsFromRecord?.let { gap ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = language.pick(
                            tr = "Rekoruna $gap saniye kaldı!",
                            en = "Only $gap seconds from your record!"
                        ),
                        color = KronColors.BlueBright,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                ResultRow(language.pick(tr = "Skor", en = "Score"), "${stats.score}")
                ResultRow(language.pick(tr = "Mesafe", en = "Distance"), "${stats.distanceMeters} m")
                ResultRow(language.pick(tr = "Süre", en = "Time"), formatTime(stats.timeSurvivedSec))
                ResultRow(
                    language.pick(tr = "Perfect Dodge", en = "Perfect Dodges"),
                    "${stats.perfectDodges}"
                )
                ResultRow(language.pick(tr = "En iyi combo", en = "Best combo"), "×${stats.bestCombo}")
                ResultRow(
                    language.pick(tr = "Kazanılan coin", en = "Coins earned"),
                    "+${result.coinsEarned + bonusCoins}"
                )
                ResultRow(language.pick(tr = "Kazanılan XP", en = "XP earned"), "+${result.xpEarned}")

                // Yildiz coini yalnizca YENI yildiz icin odenir. Bunu yazmazsak
                // ayni bolumu 3 yildizla tekrar bitiren oyuncu "yildizlarim var
                // ama coin gelmedi" diye okur.
                if (result.mode == RunMode.CAREER && result.stars > 0) {
                    ResultRow(
                        language.pick(tr = "Görev ödülü", en = "Objective reward"),
                        if (result.newStars > 0) {
                            "+${result.newStars * GameConfig.COINS_PER_STAR}"
                        } else {
                            language.pick(tr = "alınmıştı", en = "already claimed")
                        }
                    )
                }

                // Kariyerde yildiz hedefleri, gunluk gorevde kademeler ayni
                // duzende listelenir; tek fark isaret ve yanindaki coin.
                val earnedRows = when (result.mode) {
                    RunMode.CAREER -> result.stars
                    RunMode.DAILY -> result.dailyTiers
                    RunMode.ENDLESS -> 0
                }
                if (objectiveLabels.isNotEmpty() && result.mode != RunMode.ENDLESS) {
                    Spacer(modifier = Modifier.height(12.dp))
                    objectiveLabels.forEachIndexed { index, (label, progress) ->
                        val earned = index < earnedRows
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Yildiz simgesi kaldirildi: her yerde ayni dil —
                            // tamamlanan yesil tik, kalan ici bos yuvarlak.
                            ObjectiveDots(
                                earned = if (earned) 1 else 0,
                                total = 1,
                                dotSize = 15
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                color = if (earned) KronColors.TextPrimary else KronColors.TextMuted,
                                fontSize = 12.sp
                            )
                            if (progress.isNotEmpty()) {
                                Text(
                                    text = progress,
                                    color = if (earned) KronColors.ObjectiveDone else KronColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            tierRewards.getOrNull(index)?.let { reward ->
                                Text(
                                    text = "+$reward",
                                    color = if (earned) KronColors.Coin else KronColors.TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                if (result.mode == RunMode.DAILY) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when {
                            dailyReward > 0 -> language.pick(
                                tr = "Günlük görev ödülü: +$dailyReward coin",
                                en = "Daily challenge reward: +$dailyReward coins"
                            )

                            result.dailyTiers > 0 -> language.pick(
                                tr = "Bu kademeler bugün zaten alınmıştı",
                                en = "These tiers were already claimed today"
                            )

                            else -> language.pick(
                                tr = "İlk kademeye ulaşamadın — ödül yok",
                                en = "First tier not reached — no reward"
                            )
                        },
                        color = if (dailyReward > 0) KronColors.Coin else KronColors.TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }

                if (result.coinsEarned > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    // Reklamin verecegi miktar ONCEDEN kirpilarak yazilir:
                    // butonda "+500" gorup 150 almak guven kirar.
                    val bonusOffer =
                        result.coinsEarned.coerceAtMost(GameConfig.REWARDED_DOUBLE_COINS_CAP)
                    when {
                        coinsDoubled -> Text(
                            text = language.pick(
                                tr = "✓ Reklam ödülü +$bonusCoins coin",
                                en = "✓ Ad reward +$bonusCoins coins"
                            ),
                            color = KronColors.BlueBright,
                            fontSize = 13.sp
                        )

                        bonusLimitReached -> Text(
                            text = language.pick(
                                tr = "Günlük reklam ödülü sınırın doldu, yarın tekrar",
                                en = "Daily ad reward limit reached, come back tomorrow"
                            ),
                            color = KronColors.TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        else -> PrimaryButton(
                            text = if (adInFlight) {
                                language.pick(tr = "YÜKLENİYOR…", en = "LOADING…")
                            } else {
                                language.pick(
                                    tr = "REKLAM İZLE → +$bonusOffer COIN",
                                    en = "WATCH AD → +$bonusOffer COINS"
                                )
                            },
                            enabled = !adInFlight,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onDoubleCoins
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                if (hasNextLevel) {
                    PrimaryButton(
                        text = language.pick(tr = "SONRAKİ BÖLÜM", en = "NEXT LEVEL"),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNext
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                // "TEKRAR" butonu KALDIRILDI (2026-08-14): kosuyu bedavaya
                // sifirlayip yeniden baslatmak reklamsiz tur cevirmenin ve
                // kisa kosu farmlamanin en kolay yoluydu. Ayni bolumu tekrar
                // oynamak icin bolum haritasindan girilir; carpisma aninda
                // reklamli ve kosu basina bir kez "devam et" teklifi var.
                SecondaryButton(
                    text = language.pick(tr = "ANA MENÜ", en = "HOME"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onHome
                )
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(label, color = KronColors.TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = KronColors.TextPrimary, fontSize = 14.sp)
    }
}
