package com.miniappfactory.krondrive.ui.game

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.draw.alpha
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
import com.miniappfactory.krondrive.ui.common.rememberCarSprites
import com.miniappfactory.krondrive.ui.common.KronCard
import com.miniappfactory.krondrive.ui.common.KronProgressBar
import com.miniappfactory.krondrive.ui.common.PrimaryButton
import com.miniappfactory.krondrive.ui.common.SecondaryButton
import com.miniappfactory.krondrive.ui.common.ObjectiveDots
import com.miniappfactory.krondrive.ui.theme.KronColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/** HUD'daki tek bir hedef satiri: "GEÇİŞ 4/10" + durumu. */
@Immutable
private data class HudObjective(
    val label: String,
    /** "4/10" bicimi; sayilamayan hedeflerde bos. */
    val value: String,
    val met: Boolean
)

/**
 * HUD'un okudugu, kare kare degil de seyrek guncellenen ozet.
 *
 * [Immutable] SART: icindeki [objectives] bir `List` ve Compose duz `List`'i
 * kararsiz (unstable) sayar — tek bir kararsiz alan tum tipi kararsiz yapar,
 * o da [GameHud]'i "skippable degil" hale getirirdi (parametre ayni kalsa bile
 * her ust bestelemede govdesi bastan calisirdi). Buradaki soz sudur: bu nesne
 * ve icindeki liste OLUSTUKTAN SONRA DEGISMEZ — [buildHud] her seferinde yeni
 * bir HudState uretir, var olani duzenlemez. Sozu bozan bir alan eklenirse
 * (ornegin `MutableList`) annotation da kaldirilmali.
 */
@Immutable
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
    val speedLocked: Boolean = false
    // BURAYA `boostReady` EKLEME. Boost hazirligi ayri bir durumda tutuluyor
    // (bkz. GameScreen'deki `boostReadyState`), cunku onu yalnizca kontrol
    // katmani okuyor. Burada da durursa: HUD hicbir yerde gostermedigi halde,
    // deger her degistiginde HudState'in yapisal esitligi bozulur ve GameHud
    // gorunumu hic degismeyecekken yeniden bestelenir — hayalet bir tetik.
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

    /**
     * TITRESIMIN TEK KAPISI (2026-08-17).
     *
     * Ayarlarda artik bir titresim anahtari var
     * (`PlayerProgress.vibrationEnabled`, varsayilan acik). Bu ekrandaki
     * butun titresim cagrilari — serit degistirme, korna, carpisma — daha
     * once KOSULSUZDU; hepsi buradan geciyor ki tercih tek bir yerde
     * uygulansin ve yeni bir cagri eklerken unutulmasin.
     *
     * Neden [rememberUpdatedState] ve neden dogrudan `progress.x` degil:
     * asagidaki kontrol lambdalari ve kare dongusu bilerek `remember` /
     * `LaunchedEffect(engine)` icinde kuruluyor (nesne kimligi sabit kalsin
     * diye — gerekce onSteerLeft'in ustunde yazili). Tercihi degeriyle
     * yakalasalardi ayar degistiginde ESKI degeri tasirlardi. Durum nesnesi
     * okununca gecerli olan cagri anindaki degerdir.
     *
     * Okuma BESTELEME ICINDE yapilmiyor (lambda gorunumden degil, dokunma ve
     * kare geri cagrilarindan cagriliyor), yani hicbir yeniden besteleme
     * tetiklemez — 40 FPS'lik kosu bundan etkilenmez.
     */
    val vibrationEnabled = rememberUpdatedState(progress.vibrationEnabled)
    val buzz = remember(haptics) {
        { type: HapticFeedbackType ->
            if (vibrationEnabled.value) haptics.performHapticFeedback(type)
        }
    }

    // Yeniden dene: anahtari degistirmek yeni bir motor (yeni kosu) yaratir.
    var runKey by remember { mutableIntStateOf(0) }
    val engine = remember(runKey, mode, levelId) { viewModel.createEngine(mode, levelId) }

    var frame by remember(engine) { mutableIntStateOf(0) }
    // Motorun `phase` alani Compose durumu DEGIL — overlay'lerin dogru anda
    // gorunmesi icin her karede snapshot durumuna yansitiliyor.
    var phase by remember(engine) { mutableStateOf(engine.phase) }
    // DIKKAT — `by` YOK, bilerek. `hud` bu govdede OKUNMAZ; yalnizca durum
    // nesnesi asagi verilir ve [GameHud] onu kendi icinde okur.
    //
    // Eskiden `var hud by remember { ... }` idi ve govdede okunuyordu. Compose'da
    // govdedeki bir snapshot okumasi O COMPOSABLE'IN restart scope'una abone
    // olur; hud ~3 karede bir yazildigi icin GameScreen'in TAMAMI ~20 Hz'de
    // yeniden besteleniyordu (bes kontrol butonu, kaydirma katmani, overlay
    // kosullari... hepsi). "HUD'u seyrek guncelle" optimizasyonu boylece kendi
    // amacini yok ediyordu. Okumayi asagi tasimak abone kumesini HUD'a daraltir.
    val hudState = remember(engine) { mutableStateOf(HudState()) }
    // Boost hazir mi: HUD'dan AYRI bir durum, cunku bu deger kosu boyunca
    // birkac kez degisir, skor gibi surekli degil. Ayni sebeple asagida
    // YALNIZCA gercekten degistiginde yazilir — ayni degeri her karede yazmak
    // da (esitlik politikasi tutsa bile) gereksiz snapshot trafigidir.
    val boostReadyState = remember(engine) { mutableStateOf(engine.isBoostReady()) }
    // Geri sayim rakami. Motorun `countdownRemaining` alani Compose durumu
    // DEGIL; eskiden ekranda azalmasinin tek sebebi `hud` yuzunden GameScreen'in
    // 20 Hz'de yeniden bestelenmesiydi. O bagimlilik kalkinca rakam donardi,
    // bu yuzden GORUNEN saniye acikca durum olarak tutuluyor ve saniye
    // degistiginde yaziliyor (kare basina degil).
    var countdownSeconds by remember(engine) {
        mutableIntStateOf(kotlin.math.ceil(engine.countdownRemaining).toInt())
    }
    /**
     * Geri sayimda EN SON SESLENDIRILEN rakam. Compose durumu DEGIL ve
     * bilerek oyle: bu deger ekranda hicbir sey cizmiyor, yalnizca "bu rakamin
     * bipini caldim mi" sorusunu cevapliyor — snapshot durumu olsaydi her
     * yazma bos yere bir besteleme daha tetiklerdi.
     *
     * `countdownSeconds`'in kendisi bu is icin YETMEZ: ilk deger zaten 3 ile
     * baslar, yani "3" ekrana ilk geldiginde bir DEGISIM olmaz ve ilk bip hic
     * calmazdi. Buradaki [Int.MIN_VALUE] tohumu ilk karede de bir degisim
     * uretir. Kosu yeniden baslarsa (`remember(engine)`) tohum da sifirlanir.
     */
    val lastCountdownTick = remember(engine) { intArrayOf(Int.MIN_VALUE) }
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

    /**
     * Carpisma vurusunun gorsel durumu (sarsinti, flas, kivilcim).
     *
     * `by` YOK ve `mutableStateOf` YOK — bilerek. [CrashImpact] duz alanlar
     * tutar ve her karede degisir; snapshot durumu olsaydi darbe boyunca
     * GameScreen'in tamami 40 Hz'de yeniden bestelenirdi. Cizim zaten her kare
     * `frame` uzerinden gecersiz kiliniyor, yani bu nesnenin ekrani uyandirmasi
     * gerekmiyor. Ayrintili gerekce sinifin KDoc'unda.
     */
    val impact = remember(engine) { CrashImpact() }

    /**
     * Olumcul carpisma sayaci — perdenin GECIKMELI inmesini tetikler.
     *
     * Neden sayac ve neden bir boolean degil: oyuncu reklamla devam edip
     * ([RewardedAdManager] -> `engine.revive()`) tekrar carpabilir. Boolean
     * olsaydi ikinci carpismada deger zaten `true` olur, [LaunchedEffect]
     * yeniden baslamaz ve **perde bir daha hic inmezdi**. Sayac her carpismada
     * yeni bir anahtar uretir.
     */
    var crashBeat by remember(engine) { mutableIntStateOf(0) }

    val level = engine.level

    fun publishResult(runResult: RunResult) {
        if (result != null) return
        result = runResult
        showCrashDialog = false
        // Carpisma ekraninda kalmis "reklam yuklenemedi" uyarisi SONUC
        // ekranina tasinmasin: iki overlay ayni bayragi paylasiyor ve
        // oyuncu daha butona basmadan hata mesaji gormemeli.
        adFailed = false
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

                // Carpisma vurusu motordan ONCE ilerletilir. Sirasi onemli:
                // asagida bir Crash olayi gelirse `trigger` sayaci sifirlar ve
                // BU kare tam genlikte cizilir. Once adim atsaydik darbenin en
                // sert karesi bir kare gec gelirdi (~25 ms, cihazda gorulur).
                //
                // Sahne carpisma sirasinda donuyor (`step` icinde CRASHED ->
                // Unit) ama bu dongu durmuyor: `frame++` yuruyor, Canvas her
                // kare yeniden ciziliyor. Vurusun oynayabilmesinin sebebi bu.
                impact.step(dt)

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
                            // TEMAS NOKTASI. Kivilcimlar oyuncunun uzerinden
                            // degil, iki aracin ARASINDAN fiskirmali — oyuncu
                            // "neye carptim" sorusunun cevabina baksin diye.
                            //
                            // Carpilan aracin kimligi olayda tasinmiyor, ama
                            // olumcul carpismada o arac listede DURUYOR
                            // (`onCrash` silmiyor) ve carpisma anindaki en
                            // yakin arac tanim geregi odur. Second Chance
                            // durumunda motor araci olaydan ONCE siliyor, o
                            // yuzden orada oyuncunun kendi konumu kullanilir.
                            val hit = if (event.saved) {
                                null
                            } else {
                                engine.obstacles.minByOrNull {
                                    abs(it.x - engine.playerX) + abs(it.y - engine.playerY)
                                }
                            }
                            impact.trigger(
                                x = if (hit == null) engine.playerX else (engine.playerX + hit.x) / 2f,
                                y = if (hit == null) engine.playerY else (engine.playerY + hit.y) / 2f,
                                fatal = !event.saved
                            )
                            // Carpisma titresimi (docs/REVIEW_GAMEPLAY.md §4:
                            // *"titresim butcesi en anlamsiz olaya
                            // harcanmis"* — bugune kadar yalnizca serit
                            // degistirmede ve kornada vardi).
                            //
                            // [HapticFeedbackType.LongPress], Compose 1.7'de
                            // bulunan iki turden AGIR olani; digeri
                            // (`TextHandleMove`) zaten serit degistirmenin
                            // hafif tiki ve carpisma ondan ayirt edilmeli.
                            //
                            // 2026-08-17'den beri ayarlarda bir TITRESIM
                            // ANAHTARI var; cagri `buzz` uzerinden geciyor ve
                            // oyuncu kapattiysa hicbir sey olmuyor (eskiden
                            // bu satir kosulsuzdu).
                            buzz(HapticFeedbackType.LongPress)
                            // CARPISMA SESI — flasin tepe yaptigi KARE.
                            //
                            // Ayni karede motor sesi de susuyor (faz artik
                            // RUNNING degil, asagidaki `EngineSoundManager.idle()`
                            // devreye giriyor). Ikisi yarismiyor, tam tersi:
                            // motorun kesilmesi darbenin altini bosaltiyor ve
                            // vurus daha sert duyuluyor.
                            //
                            // Ses ayari kapaliysa yoneticinin kendisi zaten
                            // sessiz (setEnabled), burada ayrica kontrol yok.
                            EngineSoundManager.playCrash()
                            // Perde artik BU KAREDE inmiyor; gecikmeyi asagidaki
                            // LaunchedEffect yurutuyor.
                            if (!event.saved) crashBeat++
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

                // --- GERI SAYIM: rakam, isik ve bip TEK olaydan gelir --------
                //
                // Ekranda GORUNEN rakam degistiyse yaz. Ham float'i yazmak
                // 60 Hz'de bestelemeye donerdi; goruntu ayni, maliyet 60 kat.
                //
                // Bip ve isiklarin ayni satirdan gelmesi kasitli: ikisi ayri
                // ayri zamanlansaydi (ornegin bip bir efektten, isik
                // bestelemeden) cihazda birbirinden kayarlar ve "ses gec
                // geliyor" hissi olusurdu. Isik sayisi zaten `countdownSeconds`
                // uzerinden turetiliyor ([CountdownOverlay]), yani ikisi ayni
                // KAREDE degisiyor.
                //
                // `RunPhase.COUNTDOWN` kontrolu BILEREK YOK. Faz kontrolu
                // olsaydi son bip hic calmazdi: `step` rakami 0'a indirdigi
                // KARENIN KENDISINDE faz zaten RUNNING'e geciyor. Kontrol
                // yerine sayacin kendisi kullaniliyor ve bu bedava — geri
                // sayim bittikten sonra `countdownRemaining` 0'da sabit
                // kaldigi icin `seconds` da 0'da kalir ve asagisi bir daha hic
                // calismaz.
                val seconds = kotlin.math.ceil(engine.countdownRemaining).toInt()
                if (seconds != lastCountdownTick[0]) {
                    lastCountdownTick[0] = seconds
                    countdownSeconds = seconds
                    // seconds == 0 -> kosunun basladigi kare: uzun, bir oktav
                    // tiz "BASLA" bipi. 3/2/1 -> kisa hazirlik bipi.
                    EngineSoundManager.playCountdownBeep(final = seconds <= 0)
                }

                // Boost hazirligi ancak DEGISTIGINDE yazilir.
                val boostReady = engine.isBoostReady()
                if (boostReady != boostReadyState.value) boostReadyState.value = boostReady

                // HUD her karede degil, ~her 3 karede bir guncellenir — metin
                // yeniden bestelemesi 60 Hz'de gereksiz maliyet.
                if (frame % 3 == 0) {
                    hudState.value = buildHud(engine, language, hudState.value)
                }
                frame++
            }
        }
    }

    /**
     * CARPISMA VURUSU — perdenin gecikmesi.
     *
     * Eskiden `showCrashDialog` carpismanin oldugu `withFrameNanos` geri
     * cagrisinda yaziliyordu, yani oyuncu carpistigi kareden BIR SONRAKI
     * karede %70 opak siyah bir perde goruyordu (`OverlayScrim`). Neden
     * carptigini gorebilecegi tek bir kare yoktu — olculdu ve
     * `docs/REVIEW_GAMEPLAY.md` §6.1'de yazili.
     *
     * Simdi arada [CRASH_DIALOG_DELAY_MS] var. Bu sure boyunca:
     *  - motor DURMUS durumda (`RunPhase.CRASHED`), yani oyuncu ceza almiyor,
     *  - sahne cizilmeye devam ediyor ve carpilan arac hala orada,
     *  - kamera sarsiliyor, flas soniyor, kivilcimlar dagiliyor.
     *
     * Diyalogun KENDISI ve `revive` akisi hic degismedi: gecikme onlarin
     * onune giriyor, yerine gecmiyor.
     *
     * Ikinci titresim ([CRASH_IMPACT_HAPTIC_GAP_MS]) darbeye "curt" verir:
     * tek bir uzun titresim "bildirim" gibi okunuyor, iki kisa vurus "carptim"
     * gibi. **Cihazda denenmedi** — S8'de iki `LongPress` bu araliktan sonra
     * tek bir titresime birlesirse zarari yok, sadece tek vurus hissedilir.
     *
     * Iptal: [LaunchedEffect] besteleme kapsamiyla birlikte iptal olur (ekran
     * kapanirsa `showCrashDialog` hic yazilmaz), anahtar [crashBeat] oldugu
     * icin ikinci bir carpisma sayaci ilerletip efekti bastan baslatir.
     */
    LaunchedEffect(crashBeat) {
        if (crashBeat == 0) return@LaunchedEffect
        delay(CRASH_IMPACT_HAPTIC_GAP_MS)
        buzz(HapticFeedbackType.LongPress)
        delay(CRASH_DIALOG_DELAY_MS - CRASH_IMPACT_HAPTIC_GAP_MS)
        showCrashDialog = true
    }

    // Arac sprite'lari: yukleme Composable bir is, cizim ise her karede yurur.
    // Bu yuzden burada bir kez yuklenip ciziciye parametre olarak veriliyor.
    val carSprites = rememberCarSprites()

    // Kontrol geri cagrilari BIR KEZ kurulur (motor/haptik degismedikce ayni
    // nesne). Sebep: [DrivingControls] eskiden `engine`i parametre olarak
    // aliyordu ve `GameEngine` Compose icin kararsiz bir tip — tek basina
    // composable'i "skippable degil" yapiyordu. Ustelik cagri yerinde kurulan
    // `{ engine.steerLeft(); onSteer() }` gibi lambdalar `engine`/`haptics`
    // yakaladigi icin derleyici tarafindan hatirlanamiyor, her bestelemede YENI
    // nesne olarak iniyordu; bu da butonlarin parametrelerini "degismis"
    // gosterip bes butonu bastan kurduruyordu. Davranis birebir ayni, yalnizca
    // nesne kimligi sabit.
    val onSteerLeft = remember(engine, buzz) {
        {
            engine.steerLeft()
            buzz(HapticFeedbackType.TextHandleMove)
        }
    }
    val onSteerRight = remember(engine, buzz) {
        {
            engine.steerRight()
            buzz(HapticFeedbackType.TextHandleMove)
        }
    }
    val onBrake = remember(engine) { { held: Boolean -> engine.setBrake(held) } }
    val onBoost = remember(engine) { { held: Boolean -> engine.setBoost(held) } }
    val onHorn = remember(buzz) {
        {
            // Bekleme suresi dolmadiysa ses calmaz; titresim de ancak ses
            // gercekten caldiysa verilir.
            if (EngineSoundManager.playHorn()) {
                buzz(HapticFeedbackType.TextHandleMove)
            }
            Unit
        }
    }
    val onPauseTap = remember(engine) {
        {
            if (engine.phase == RunPhase.RUNNING) {
                engine.pause()
                paused = true
            }
        }
    }

    /**
     * Sistem geri tusu KOSUYU SESSIZCE SILIYORDU (2026-08-16'da bulundu).
     *
     * Projede hicbir `BackHandler` yoktu: sürerken geri tusuna basmak ekrani
     * kapatiyor, `onRunFinished` hic cagrilmiyor ve o kosunun coin'i, XP'si,
     * gorev ilerlemesi UYARISIZ gidiyordu. Duraklat menusunden cikis yolu
     * ise tam tersini yapiyor (sonucu yayimliyor) — yani ayni niyetin iki
     * yolu iki farkli sonuc veriyordu.
     *
     * Cozum en az sasirtan davranis: geri tusu DURAKLATIR. Oyuncu cikmak
     * isterse duraklatma menusundeki cikisi kullanir, o da sonucu kaydeder.
     * Duraklatilmisken ikinci kez basmak devam ettirir.
     */
    BackHandler(enabled = engine.phase == RunPhase.RUNNING || paused) {
        if (paused) {
            engine.resume()
            paused = false
        } else {
            onPauseTap()
        }
    }
    val onToggleSpeedLock = remember(engine) {
        {
            engine.toggleSpeedLock()
            // HUD ~3 karede bir guncelleniyor; kilit durumu ANINDA gorunsun
            // diye burada da yansitiliyor.
            hudState.value = hudState.value.copy(speedLocked = engine.speedLocked)
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
                gaugeSmallSize = 9.sp,
                sprites = carSprites,
                // Sonmusken hicbir ek is yapmaz; kosu sirasindaki maliyeti
                // tek bir boolean okumasi.
                impact = impact
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
                                buzz(HapticFeedbackType.TextHandleMove)
                                accumulated = 0f
                            } else if (accumulated <= -threshold) {
                                engine.steerLeft()
                                buzz(HapticFeedbackType.TextHandleMove)
                                accumulated = 0f
                            }
                        }
                    }
            )
            DrivingControls(
                language = language,
                // Durum NESNESI iniyor, degeri degil: okuma asagida yapilir,
                // boylece boost hazirligi degistiginde GameScreen degil yalnizca
                // kontrol katmani yeniden bestelenir.
                boostReady = boostReadyState,
                // Korna oynanisi HIC etkilemez; ses kapaliyken buton hic
                // gosterilmez (olu butona basmak "bozuk" hissi verir).
                hornAvailable = progress.soundEnabled,
                onSteerLeft = onSteerLeft,
                onSteerRight = onSteerRight,
                onBrake = onBrake,
                onBoost = onBoost,
                onHorn = onHorn,
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
            hudState = hudState,
            language = language,
            onPause = onPauseTap,
            onToggleSpeedLock = onToggleSpeedLock,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        )


        if (phase == RunPhase.COUNTDOWN) {
            CountdownOverlay(countdownSeconds, language)
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
                    withOptionalInterstitial(viewModel, mode, levelId, activity) { onExit() }
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
                adFailed = adFailed,
                onDoubleCoins = {
                    if (activity != null && !adInFlight) {
                        adInFlight = true
                        // Yeni denemede eski hata mesaji silinir.
                        adFailed = false
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
                            // Sessiz basarisizlik yok (docs/REVIEW_UX.md §4):
                            // buton eski etiketine donuyor ama coin gelmiyordu,
                            // ekran da hicbir sey demiyordu — okunusu "buton
                            // bozuk" idi. Ayni desen zaten CrashOverlay'de var.
                            onFailure = { adFailed = true },
                            onAdClosed = { adInFlight = false }
                        )
                    }
                },
                onNext = {
                    // Esik BITEN bolume gore: 4. bolumu bitirip 5'e gecerken
                    // reklam cikmaz, cikaran kosu hala reklamsiz bolgedeydi.
                    withOptionalInterstitial(viewModel, mode, runResult.levelId, activity) {
                        nextLevelId?.let(onPlayLevel)
                    }
                },
                onHome = {
                    withOptionalInterstitial(viewModel, mode, runResult.levelId, activity) {
                        onExit()
                    }
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
    levelId: Int?,
    activity: Activity?,
    proceed: () -> Unit
) {
    val shouldShow = GameConfig.INTERSTITIAL_AFTER_EVERY_RUN ||
        viewModel.shouldShowInterstitial(mode, levelId)
    if (activity != null && shouldShow) {
        viewModel.onInterstitialShown(mode)
        InterstitialAdManager.loadAndShow(activity, activity) { proceed() }
    } else {
        proceed()
    }
}

/**
 * Motorun anlik durumundan HUD ozeti uretir.
 *
 * [previous] bir onceki ozettir ve tek isi LISTE KIMLIGINI korumaktir: hedef
 * satirlari kosunun buyuk bolumunde hic degismez, ama `map` her cagrida yeni
 * bir liste nesnesi dondurur. Icerik ayniyken eskisini geri vermek hem 3 karede
 * bir yapilan gereksiz tahsisi keser hem de [HudObjective] listesini alan alt
 * composable'larin ("ayni nesne mi?") kisa yolundan gecmesini saglar.
 */
private fun buildHud(
    engine: com.miniappfactory.krondrive.game.GameEngine,
    language: AppLanguage,
    previous: HudState
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
        objectives = if (objectives == previous.objectives) previous.objectives else objectives,
        speedLockAvailable = engine.canLockSpeed(),
        speedLocked = engine.speedLocked
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

/**
 * Carpisma ile "ÇARPTIN!" perdesinin inmesi arasindaki sure.
 *
 * 300 ms nereden: hedef cihazda (~40 FPS) **12 kare**, yani goz bir sahneyi
 * okumaya yetecek kadar. Daha kisasi (150 ms) tek goz kirpmasinin altinda
 * kalir ve bugunku "hicbir sey gormedim" sikayetini cozmez; daha uzunu
 * (500 ms+) oyuncunun devam etmek istedigi anda oyunun takildigi hissini
 * verir — kaybettigini zaten anlamis oyuncuyu bekletmek ceza gibi okunur.
 *
 * Bu sure icinde motor DURMUS durumdadir ([RunPhase.CRASHED]), yani oyuncu
 * hicbir sey kaybetmez; yalnizca perde bekler.
 *
 * Sarsinti (200 ms) ve flas (160 ms) bu surenin ICINDE biter — geriye perde
 * inmeden once sakin bir kare kalir. Bkz. `GameRenderer.SHAKE_SEC`.
 */
private const val CRASH_DIALOG_DELAY_MS = 300L

/** Darbenin ikinci titresimi (bkz. carpisma [LaunchedEffect]'i). */
private const val CRASH_IMPACT_HAPTIC_GAP_MS = 80L

private fun formatTime(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

// ---------------------------------------------------------------------------
// HUD
// ---------------------------------------------------------------------------

/**
 * HUD. Ozeti DEGER olarak degil DURUM olarak alir ([State]): okuma burada,
 * yani GameScreen'in restart scope'unda degil bu composable'inkinde yapilir.
 * Boylece ~3 karede bir gelen HUD guncellemesi yalnizca bu agaci yeniler;
 * kontrol butonlari, kaydirma katmani ve overlay kosullari disarida kalir.
 *
 * Parametrelerin hepsi kararli ([State] `@Stable`, [HudState] `@Immutable`,
 * geri cagrilar cagri yerinde `remember`lanmis) — yani bu composable
 * skippable: HUD disinda bir sey degistiginde govdesi hic calismaz.
 */
@Composable
private fun GameHud(
    hudState: State<HudState>,
    // AppLanguage bir enum, yani KARARLI — composable skippable kalir.
    language: AppLanguage,
    onPause: () -> Unit,
    onToggleSpeedLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hud = hudState.value

    // Panel YOK: gostergeler dogrudan yolun uzerinde, sadece golgeyle okunakli.
    // Oyuncu geri bildirimi (2026-08-13): ustteki koyu kutu hizlandikca yolu
    // goremeyecek kadar cok yer kapliyordu. Simdi skor sol ust, sure/hedef sag
    // ust kosede; boost ise ekranin en ustunde ince bir serit.
    // Stil sabitlerden kuruluyor, o yuzden bir kez: HUD saniyede ~20 kez
    // yenileniyor ve her seferinde ayni TextStyle'i tahsis etmenin anlami yok.
    val hudText = remember {
        TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            shadow = Shadow(Color(0xCC000000), Offset(0f, 2f), 4f)
        )
    }

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
            // Duraklat 36 -> 48 dp, glif 14 -> 20 sp (2026-08-16, sahibi:
            // "pause tusunu buyut"). 36 dp, Android'in onerdigi 48 dp'lik
            // asgari dokunma hedefinin ALTINDAYDI — ekranin tepesinde, surus
            // sirasinda ve tek elle basilan bir tus icin fazla kucuktu.
            Box(
                modifier = Modifier
                    .size(PAUSE_BUTTON_SIZE)
                    .background(Color(0x66040C16), RoundedCornerShape(14.dp))
                    .pointerInput(Unit) { detectTapGestures(onPress = { onPause() }) },
                contentAlignment = Alignment.Center
            ) {
                Text("II", color = KronColors.Accent, fontSize = 20.sp)
            }

            // Hiz kilidi YALNIZCA sonsuz modda (sahibi istegi, 2026-08-15):
            // "uzun sure gitmek isteyen varsa sabitlesin, yavas yavas gitsin".
            // Kariyer ve gunluk gorevde hedefler sureye/hiza bagli oldugu icin
            // boyle bir dugme dengeyi bozardi, o yuzden hic gosterilmiyor.
            if (hud.speedLockAvailable) {
                Box(
                    modifier = Modifier
                        .heightIn(min = PAUSE_BUTTON_SIZE)
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
                        // Katalogdaki TEK tek-dilli sabit metindi (2026-08-16):
                        // Ingilizce oyuncu "HIZ" goruyordu. Kilit emojisi de
                        // kaldirildi — ayni gun kontrol tuslarindaki emojiler
                        // cizime gecirilmisti, bu onlardan arta kalmisti.
                        text = language.pick(
                            tr = if (hud.speedLocked) "HIZ ✓" else "HIZ",
                            en = if (hud.speedLocked) "SPEED ✓" else "SPEED"
                        ),
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
/**
 * Fren ve boost olcusu. 2026-08-14'te 88 -> 76 -> 64 dp'ye indi ve **oyle
 * kaliyor** — sahibi 2026-08-16'da acikca "fren boost buyuklugu aynen kalsin"
 * dedi.
 */
private val PEDAL_SIZE = 64.dp

/** HUD'daki duraklat tusu. 36 -> 48 dp (2026-08-16, sahibi istegi). */
private val PAUSE_BUTTON_SIZE = 48.dp

/**
 * Yon (sol/sag) olcusu. **64 -> 76 dp** (2026-08-16, sahibi istegi: *"sag sol
 * yon tusu biraz daha buyuk olsun fren ve boost a gore"*).
 *
 * Yon butonu fren/boost'tan farkli calisiyor: fren/boost BASILI TUTULUYOR
 * (parmak yerinde durur), yon butonuna ise ard arda ve aceleyle DOKUNULUYOR.
 * Acele dokunusun isabet orani hedefin alaniyla artar, basili tutmanin
 * artmaz — yani ikisinin ayni boyda olmasi icin bir sebep yoktu.
 *
 * Buyuyen buton sutunu KAYDIRMIYOR: asagida yon butonunun kenar dolgusu
 * fren/boost'un MERKEZINE gore hesaplaniyor ([STEER_EDGE_PADDING]), yoksa
 * 12 dp'lik fark butonu ekran kenarina dogru iterdi.
 */
private val STEER_SIZE = 76.dp

private val CONTROL_EDGE_PADDING = 16.dp
private val CONTROL_BOTTOM_PADDING = 24.dp

/** Fren/boost sutununun merkezi, ekran kenarindan. Hizalamanin dayanagi. */
private val CONTROL_COLUMN_CENTER = CONTROL_EDGE_PADDING + PEDAL_SIZE / 2f

/** Yon butonu sutun merkezine oturur; buyudugunde iki yana esit tasar. */
private val STEER_EDGE_PADDING = CONTROL_COLUMN_CENTER - STEER_SIZE / 2f

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

/** Cizilen korna ikonunun kenari. Buton 48 dp; ikon kenarlara yapismasin. */
private val HORN_ICON_SIZE = 26.dp
private val HORN_TOP = Color(0xFF6E5B2C)
private val HORN_BOTTOM = Color(0xFF251F10)

/** Korna, fren/boost ile dikeyde ayni hizada dursun diye. */

/**
 * Ust kenardaki cam parlamasi ve dis cerceve.
 *
 * Parlama 0x59 -> 0x73 arttirildi (2026-08-16, referans butonlar): oradaki
 * butonlar daha "cam" duruyordu. [CONTROL_HIGHLIGHT_MID] parlamanin tepede
 * birden kesilmesini engelleyen ara duraktir — tek durakla gecis sert bir
 * yarim ay gibi goruluyor.
 */
private val CONTROL_HIGHLIGHT = Color(0x73FFFFFF)
private val CONTROL_HIGHLIGHT_MID = Color(0x26FFFFFF)
private val CONTROL_BORDER = Color(0x40FFFFFF)

/**
 * Cam parlamasinin fircasi. Duraklari SABIT — hicbir parametreye bagli degil,
 * o yuzden butonun icinde `remember` ile degil DOSYA DUZEYINDE tutuluyor:
 * bes butonun hepsi tek nesneyi paylasir. Icerde `remember` iken bu tahsis
 * hem buton basina ayri yapiliyordu hem de cagri `if (!pressed)` blogunun
 * icinde oldugu icin her basip birakmada grup atilip yeniden kuruluyordu.
 *
 * Boyuttan bagimsiz olmasi tesadufi degil: duraksiz/bitis-Y'si verilmemis bir
 * `verticalGradient` gercek yuksekligi CIZIM aninda kutudan alir, bu yuzden
 * ayni nesne farkli boyuttaki butonlarda (64 dp kontroller, 48 dp korna)
 * dogru sonuc verir.
 */
private val CONTROL_GLASS_BRUSH = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to CONTROL_HIGHLIGHT,
        0.22f to CONTROL_HIGHLIGHT_MID,
        0.45f to Color.Transparent,
        1f to Color.Transparent
    )
)

/**
 * Kontrol ikonlari GLIF degil CIZIM (2026-08-16, sahibi: *"sag sol
 * butonlarinin yerine dandik icon koymussunuz, boost tusundaki simsek
 * neredeyse gozukmuyor"*).
 *
 * Eskiden yonler "◀ ▶", boost "⚡" karakteriyle basiliyordu. Uc ayri sorun:
 *  - Glif her Android surumunde baska fontla ciziliyor; hedef cihaz API 24
 *    ve orada "◀ ▶" ince, keskin ve kucuk bir ucgen olarak geliyor.
 *  - "⚡" bir EMOJI: sistem onu kendi renginde (mavimsi/sari) ciziyor, biz
 *    rengini belirleyemiyoruz — mavi boost zemininde eriyip kayboluyordu.
 *  - Boyut/agirlik fontun elinde; butonun kabartmali diline uymuyordu.
 *
 * Cizilen ikon her cihazda ayni, rengini biz veriyoruz ve butonun kendi dikey
 * gradyanini yankilayabiliyor. [HornIcon] bu isi zaten boyle yapiyordu; yon,
 * boost ve fren de artik ayni dilde.
 */
// Buton 64 -> 76 dp buyudu; ikon ayni oranda (30 -> 36) buyuyor ki buton
// icinde "kaybolmus" gorunmesin (2026-08-16).
private val STEER_ICON_SIZE = 36.dp
private val BOOST_ICON_SIZE = 28.dp

/** Ikon govdesi: ust beyaz, alt gri-mavi — butonun gradyaniyla ayni yon. */
private val ICON_FILL_TOP = Color(0xFFFFFFFF)
private val ICON_FILL_BOTTOM = Color(0xFFC3D2E4)

/** Yon oklari ve fren gibi notr ikonlarin varsayilan govde gradyani. */
private val ICON_FILL = listOf(ICON_FILL_TOP, ICON_FILL_BOTTOM)

/**
 * Boost simseginin SARI govdesi (2026-08-16, sahibinin gonderdigi referans
 * butonlar): simsek beyaz degil, ustte acik sari altta turuncumsu.
 *
 * Tonlar oyunun kendi sarisindan geliyor ki simsek yabanci bir renk gibi
 * durmasin: orta durak dogrudan [KronColors.AccentBright] (#FFD43D — arac
 * alevinin ve surucu kaskinin rengi), ust durak ayni tonun acilmis hali, alt
 * durak da [KronColors.Accent] (#F5C100) tonunun turuncuya cekilmisi. Ucu bir
 * arada simsege "kendi isigi var" hissi veriyor; tek duz sari yassi duruyordu.
 */
private val BOLT_FILL = listOf(
    Color(0xFFFFEE9B),
    KronColors.AccentBright,
    Color(0xFFE07C0C)
)

/**
 * Ikonun altina dusen yumusak golge — referans butonlarda ikonlar zeminin
 * uzerinde DURUYOR gibi, icine gomulu degil. Konturun disina tastigi icin
 * [iconBounds] bu payi da hesaba katar, yoksa golge butonun kenarinda kirpilir.
 */
private val ICON_SHADOW = Color(0x59020814)
private const val ICON_SHADOW_OFFSET = 0.9f

/**
 * Ikonun koyu konturu. Asil isi BOOST: #2189D6 mavi zeminde duz beyaz bir
 * simsek zemine karisiyordu. Kontur simsegin cevresini zeminden koparip
 * silueti her zeminde okunur yapiyor; ayni kontur yon oklarinda da var ki
 * ikonlar tek aileden gorunsun.
 */
private val ICON_CONTOUR = Color(0xD90A1524)

/** Kontur kalinligi (ikon koordinat kutusu birimi). */
private const val ICON_CONTOUR_WIDTH = 1.5f

/** Dolu ikonlarda keskin koseleri sisirip yumusatan yuvarlatma payi. */
private const val ICON_ROUNDING = 1.6f

/** Yon butonu, alttaki fren/boost butonunun tam ustunde durur. */
private val STEER_BOTTOM_PADDING =
    CONTROL_BOTTOM_PADDING + PEDAL_SIZE + CONTROL_VERTICAL_GAP

/**
 * Korna SAG SUTUNDA, yon okunun hemen USTUNDE (2026-08-16, proje sahibi:
 * *"kornayi sag yonlendirme okunun biraz ustune koyalim cunku zaten bir
 * fonksiyonu yok"*). Onceden ekranin alt ORTASINDAYDI; orasi oynanis alaninin
 * tam altinda en degerli yer ve dekoratif bir tusa ayrilmasi dogru degildi.
 *
 * Yukseklik: boost -> yon oku -> korna diye ustuste. Yatayda korna sutunun
 * ORTASINA hizalanir, yoksa kenara yapisik durur.
 *
 * **Korna ile yon oku arasindaki bosluk AYRI ve GENIS** (2026-08-16, sahibi:
 * *"kornayi biraz yukari alman lazim, sag yon tusu ile arasinda bosluk kalsin
 * ki yanlislikla ona basilmasin"*). Diger butonlar arasindaki 5 dp bilincli
 * olarak dardi — parmak fren ile yon arasinda kaydirarak gitsin diye. Korna
 * icin bunun tersi gecerli: oynanisa etkisi olmayan dekoratif bir tusa
 * yanlislikla basmak, yon vermeye calisirken kaybedilen bir kare demek.
 * O yuzden korna kendi bosluguyla ([HORN_GAP]) ayriliyor.
 */
private val HORN_GAP = 24.dp

private val HORN_BOTTOM_PADDING =
    STEER_BOTTOM_PADDING + STEER_SIZE + HORN_GAP

private val HORN_EDGE_PADDING = CONTROL_COLUMN_CENTER - HORN_SIZE / 2f

/**
 * Kontrol katmani.
 *
 * Parametrelerin hepsi KARARLI olmak zorunda, yoksa composable skippable
 * olmaz ve her ust bestelemede bes butonun tamami (golge, iki gradyan, ikon
 * Canvas'i) bastan kurulur. Bu yuzden:
 *  - `engine` artik BURAYA GIRMEZ (kararsiz sinif); yerine cagri yerinde
 *    `remember`lanmis geri cagrilar iniyor.
 *  - [boostReady] deger degil DURUM: okumasi bu govdede yapiliyor, boylece
 *    boost hazirligi degistiginde GameScreen degil sadece burasi yenileniyor.
 */
@Composable
private fun DrivingControls(
    language: AppLanguage,
    boostReady: State<Boolean>,
    hornAvailable: Boolean,
    onSteerLeft: () -> Unit,
    onSteerRight: () -> Unit,
    onBrake: (Boolean) -> Unit,
    onBoost: (Boolean) -> Unit,
    onHorn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boostIsReady = boostReady.value
    Box(modifier = modifier) {
        ControlButton(
            topColor = STEER_TOP,
            bottomColor = STEER_BOTTOM,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = STEER_EDGE_PADDING, bottom = STEER_BOTTOM_PADDING)
                .size(STEER_SIZE),
            onPress = onSteerLeft
        ) {
            ArrowIcon(pointsRight = false, modifier = Modifier.size(STEER_ICON_SIZE))
        }
        ControlButton(
            topColor = STEER_TOP,
            bottomColor = STEER_BOTTOM,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = STEER_EDGE_PADDING, bottom = STEER_BOTTOM_PADDING)
                .size(STEER_SIZE),
            onPress = onSteerRight
        ) {
            ArrowIcon(pointsRight = true, modifier = Modifier.size(STEER_ICON_SIZE))
        }
        HoldButton(
            topColor = BRAKE_TOP,
            bottomColor = BRAKE_BOTTOM,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = CONTROL_EDGE_PADDING, bottom = CONTROL_BOTTOM_PADDING)
                .size(PEDAL_SIZE),
            onHoldChanged = onBrake
        ) {
            // Fren METIN kaliyor, ikon olmuyor. Denenen ikonlar (fren diski,
            // sekizgen dur levhasi, pedal) 64 dp'de ya "durdur/duraklat" ya da
            // "ayar" okunuyordu; yazi belirsizlik birakmiyor ve iki dilde de
            // dar (FREN/BRAKE, 4-5 harf). Tek eksigi kirmizi zeminde beyazin
            // biraz yumusak kalmasiydi: ikonlardaki koyu konturun metin
            // karsiligi olarak yaziya da golge eklendi.
            ControlLabel(language.pick(tr = "FREN", en = "BRAKE"))
        }
        HoldButton(
            // Boost bosaldiginda ve parmak basiliyken KILITLENIR (bkz.
            // GameConfig.BOOST_REENGAGE_MIN). Buton eskiden bunu hic belli
            // etmiyordu: oyuncu basiyor, hicbir sey olmuyor ve "boost yaptim
            // ama saymadi" diyordu (2026-08-15). Artik hazir degilken soluk.
            topColor = if (boostIsReady) BOOST_TOP else BOOST_EMPTY_TOP,
            bottomColor = if (boostIsReady) BOOST_BOTTOM else BOOST_EMPTY_BOTTOM,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = CONTROL_EDGE_PADDING, bottom = CONTROL_BOTTOM_PADDING)
                .size(PEDAL_SIZE),
            onHoldChanged = onBoost
        ) {
            BoltIcon(ready = boostIsReady, modifier = Modifier.size(BOOST_ICON_SIZE))
        }
        if (hornAvailable) {
            HornButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = HORN_EDGE_PADDING, bottom = HORN_BOTTOM_PADDING)
                    .size(HORN_SIZE),
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
    // Gradyanlar `remember`li: girdileri yalnizca renkler ve basili olup
    // olmadigi. Bunsuz her bestelemede yeni bir liste + yeni bir Brush
    // tahsis ediliyordu ve bes butonun hepsinde ikiser tane. Duraklar ve
    // yon birebir ayni, yalnizca nesne yeniden kullaniliyor.
    //
    // NOT: duraksiz `verticalGradient` boyutunu CIZIM aninda kutudan alir
    // (0..POSITIVE_INFINITY), bu yuzden hatirlamak boyut degisiminde bile
    // yanlis sonuc uretmez.
    val bodyBrush = remember(topColor, bottomColor, pressed) {
        Brush.verticalGradient(
            if (pressed) listOf(bottomColor, topColor) else listOf(topColor, bottomColor)
        )
    }
    Box(
        modifier = modifier
            .shadow(elevation, CONTROL_SHAPE)
            .background(brush = bodyBrush, shape = CONTROL_SHAPE)
            .border(1.dp, CONTROL_BORDER, CONTROL_SHAPE)
            // Cocuklar da daireye kirpilsin. Bunsuz, ic katmanlarin kendi
            // kirpmasi butonun disina tasabiliyor (asagidaki nota bak).
            .clip(CONTROL_SHAPE),
        contentAlignment = Alignment.Center
    ) {
        // Ust yaridaki cam parlamasi.
        //
        // DIKKAT — burasi 2026-08-15'te bir hata kaynagiydi: parlama kutusu
        // butonun ust %45'ini kapliyor ve KENDINI [CONTROL_SHAPE] ile
        // kirpiyordu. Ama [CircleShape] = %50 kose yaricapi, yani 64x28.8 dp
        // bir kutuda DAIRE degil HAP sekli uretiyor. Hapin ust koseleri
        // dairenin omuzlarindan tasiyor ve gece temasinda butonun arkasinda
        // "yuvarlak kare bir golge cerceve" gibi gorunuyordu (sahibi
        // bildirdi; gölge sanildi, elevation 0 yapilinca leke DURDU — sucun
        // burada oldugu boyle bulundu).
        //
        // Cozum: kutu butonun TAMAMINI kaplar (kare -> kirpma gercek daire),
        // sonumlenmeyi gradyan duraklari yapar.
        if (!pressed) {
            // Firca [CONTROL_GLASS_BRUSH]'ta, dosya duzeyinde: parametresiz
            // oldugu icin burada hatirlanmasinin bir faydasi yok, zarari var
            // (bkz. oradaki not).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CONTROL_SHAPE)
                    .background(CONTROL_GLASS_BRUSH)
            )
        }
        content()
    }
}

/**
 * Tek dokunuslu kontrol butonu. Icerigi CAGIRAN veriyor (eskiden bir [String]
 * etiketi aliyordu): ikonlar artik cizim, metin degil.
 */
@Composable
private fun ControlButton(
    topColor: Color,
    bottomColor: Color,
    modifier: Modifier,
    onPress: () -> Unit,
    content: @Composable () -> Unit
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
        },
        content = content
    )
}

// ---------------------------------------------------------------------------
// Kontrol ikonlari (hepsi CIZIM — gerekcesi icin bkz. [STEER_ICON_SIZE])
// ---------------------------------------------------------------------------

/**
 * Ikonu Canvas'in ortasina oturtur: [bounds] kutusu tuvale sigacak sekilde
 * olceklenir ve ortalanir.
 *
 * Ortalamanin ELDE yapilmasi 2026-08-16'da korna ikonunu butonun sag altina
 * kacirmisti; o yuzden her ikon sinirini kendi cizimden turetir ve buraya
 * verir.
 */
private fun DrawScope.drawFitted(bounds: Rect, block: DrawScope.() -> Unit) {
    val scale = size.minDimension / maxOf(bounds.width, bounds.height)
    translate(
        left = size.width / 2f - bounds.center.x * scale,
        top = size.height / 2f - bounds.center.y * scale
    ) {
        scale(scale, scale, pivot = Offset.Zero) { block() }
    }
}

/**
 * Cizgi ikonlarinda govdenin gercek kalinligi; dolu ikonlarda ([bodyWidth] 0)
 * yalnizca koseleri yumusatan [ICON_ROUNDING] kadar sisirme yapilir.
 */
private fun iconBodyStroke(bodyWidth: Float): Float =
    if (bodyWidth > 0f) bodyWidth else ICON_ROUNDING

/**
 * Ikonun kaplayacagi kutu: cizimin kendi siniri + disari tasan firca payi
 * (govdenin yarisi + kontur + golge kaymasi).
 *
 * Golge payi DORT YANDAN birden ekleniyor, yalniz alttan degil: tek yandan
 * eklemek kutunun merkezini kaydirir ve [drawFitted] ikonu butonda merkezden
 * kacirir. Dort yandan esit pay ikonu bir tik kucultur ama ortali birakir.
 *
 * TUZAK: `Path.getBounds()` bir YAY icin yayin degil TAM CEMBERIN kutusunu
 * verir. Burada gecen yollarin hepsi DUZ CIZGI oldugu icin sonuc dogru;
 * yay eklenirse sinir elle hesaplanmali (ornegi [HornIcon] icinde).
 */
private fun iconBounds(path: Path, bodyWidth: Float = 0f): Rect =
    path.getBounds()
        .inflate(iconBodyStroke(bodyWidth) / 2f + ICON_CONTOUR_WIDTH + ICON_SHADOW_OFFSET)

/**
 * Ikonlarin ortak boyamasi: GOLGE + KOYU KONTUR + gradyan govde.
 *
 * Tek yerde durmasinin sebebi yon oklari ile boost simseginin ayni dili
 * konusmasi; ayrilan tek sey [fillColors], cunku simsek sari, oklar beyaz.
 *
 * Sira onemli: once golge, sonra yolun KALIN (kontur) hali, en uste govde.
 * Govde konturun ustune binince kenarda [ICON_CONTOUR_WIDTH] kadar koyu bir
 * cerceve kalir. Kapali yollarda ayrica ic dolgu gerekir; acik yolda dolgu
 * ucgen bir leke uretecegi icin kosullu.
 *
 * [bodyWidth] 0 iken sekil DOLUDUR: govde firca izi yalnizca [ICON_ROUNDING]
 * kadar sisirme yapar, yani keskin koseler yuvarlanir. Dolu yon ucgeninin
 * yuvarlak koseleri buradan geliyor.
 */
private fun DrawScope.drawIconShape(
    path: Path,
    bounds: Rect,
    bodyWidth: Float = 0f,
    fillColors: List<Color> = ICON_FILL
) {
    val body = iconBodyStroke(bodyWidth)
    val outline = Stroke(
        width = body + ICON_CONTOUR_WIDTH * 2f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    val bodyStroke = Stroke(width = body, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val fill = Brush.verticalGradient(
        colors = fillColors,
        startY = bounds.top,
        endY = bounds.bottom
    )
    // Golge: sekli oldugu gibi asagi kaydirip soluk koyu cizmek yeter — ayri
    // bir blur katmani 60 Hz'de bedava degil, kontur zaten kenari kesiyor.
    translate(top = ICON_SHADOW_OFFSET) {
        drawPath(path, ICON_SHADOW, style = outline)
        if (bodyWidth == 0f) drawPath(path, ICON_SHADOW)
    }
    drawPath(path, ICON_CONTOUR, style = outline)
    if (bodyWidth == 0f) drawPath(path, fill)
    drawPath(path, fill, style = bodyStroke)
}

/**
 * Yon oku: DOLU, koseleri yuvarlatilmis ucgen — klasik "play" ucgeni.
 *
 * Once chevron'du (aralikli, kalin bir "›"). Sahibinin 2026-08-16'da
 * gonderdigi referans butonlarda ok dolu bir ucgen: chevron'a gore daha cok
 * murekkep tasidigi icin 64 dp'lik butonda uzaktan daha erken okunuyor ve
 * mavi/koyu zeminde silueti daha net.
 *
 * "Dandik" bulunan eski "◀ ▶" glifi ile karistirilmasin: sorun ucgen OLMASI
 * degil, glifin ince/keskin cizilmesi ve rengini font'un secmesiydi. Buradaki
 * ucgen cizim: koseleri [ICON_ROUNDING] kadar sisirilerek yuvarlatiliyor,
 * kendi koyu konturu ve golgesi var ([drawIconShape]).
 *
 * Sol ok, sag okun AYNASI: ayri bir yol kurulsa iki yonun orani zamanla
 * birbirinden kayardi.
 *
 * Koordinatlar 24x24 kutuda; [drawFitted] gercek boyuta olcekler.
 */
@Composable
private fun ArrowIcon(pointsRight: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Genislikten uzun (10 x 16.8): esit kenarli bir ucgen "oynat" degil
        // "yukari/asagi" da okunabiliyordu; uzun kenar yonu tekil kiliyor.
        val path = Path().apply {
            moveTo(8.4f, 3.6f)
            lineTo(18.0f, 12.0f)
            lineTo(8.4f, 20.4f)
            close()
        }
        val bounds = iconBounds(path)
        drawFitted(bounds) {
            scale(if (pointsRight) 1f else -1f, 1f, pivot = bounds.center) {
                drawIconShape(path, bounds)
            }
        }
    }
}

/**
 * Boost simsegi. Dolu SARI govde + koyu kontur ([drawIconShape]).
 *
 * Sahibin ilk sikayeti buydu: "⚡" emojisi #2189D6 mavi zeminde neredeyse
 * gorunmuyordu. Emoji yerine cizim geldi ve govde beyaz yapildi. Ikinci turda
 * (referans butonlar, 2026-08-16) beyaz da SARI oldu: sari maviyle tamamlayici
 * oldugu icin ayni siluet mavi zeminden cok daha erken kopuyor, ustelik
 * "enerji" isareti olarak beyazdan daha dogru okunuyor. Tonlar icin bkz.
 * [BOLT_FILL] — hepsi oyunun kendi sarisindan.
 *
 * Koyu kontur KALIYOR: sari, butonun acik ust gradyanina (#2189D6'nin isikli
 * yarisi) yaklastigi yerde kontursuz kenari kaybediyordu.
 *
 * Centikler bilerek asimetrik (solda 13.2, sagda 10.6): simetrik olsa simsek
 * degil kum saati okunuyor.
 */
@Composable
private fun BoltIcon(ready: Boolean, modifier: Modifier = Modifier) {
    // Hazir degilken ikon da soluyor. Buton govdesi 2026-08-15'te bunu zaten
    // renkle soyluyordu; ama yeni simsek eski emojiden cok daha baskin, tam
    // parlaklikta kalsaydi "boost hazir" izlenimi verip o uyariyi bogardi.
    Canvas(modifier = modifier.alpha(if (ready) 1f else 0.45f)) {
        val path = Path().apply {
            moveTo(14.4f, 2.0f)
            lineTo(6.4f, 13.2f)
            lineTo(11.0f, 13.2f)
            lineTo(9.4f, 22.0f)
            lineTo(17.6f, 10.6f)
            lineTo(12.8f, 10.6f)
            close()
        }
        val bounds = iconBounds(path)
        drawFitted(bounds) {
            drawIconShape(path, bounds, fillColors = BOLT_FILL)
        }
    }
}

/**
 * Korna butonu. Kontrollerin hepsi cizilmis ikon kullaniyor; korna bu isi ilk
 * yapan oldu — eskiden 📣 (megafon emojisi) vardi ve proje sahibi bunun korna
 * gibi durmadigini soyledi (2026-08-16).
 *
 * Cozum emoji degistirmek degil, ikonu CIZMEK: emoji her Android surumunde
 * baska turlu ciziliyor (renkli, farkli oran) ve diger tuslarin duz beyaz
 * dilinden kopuyordu. Cizilen ikon her cihazda ayni.
 */
@Composable
private fun HornButton(modifier: Modifier, onPress: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    RaisedControl(
        topColor = HORN_TOP,
        bottomColor = HORN_BOTTOM,
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
        HornIcon(modifier = Modifier.size(HORN_ICON_SIZE))
    }
}

/**
 * Ampullu korna (klakson) ikonu — huni + lastik ampul + iki ses dalgasi.
 *
 * Neden ampul: huni tek basina MEGAFON okunuyor (degistirmek istedigimiz
 * seyin ta kendisi). Ampul onu tartismasiz korna yapiyor ve oyunun retro
 * dilini de tutuyor.
 *
 * Koordinatlar 24x24'luk bir kutuda; butonun boyutuna gore olcekleniyor.
 */
@Composable
private fun HornIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        // Gövde TEK yol olarak kuruluyor (huni + boyun + ampul birlesik):
        // ayri ayri cizilince parcalarin kesistigi yerde kenar dikisleri
        // goruluyordu.
        val body = Path().apply {
            // Huni: dar boyundan agza acilir; agiz disa dogru kavisli.
            moveTo(10.4f, 11.8f)
            lineTo(16.8f, 4.0f)
            quadraticTo(21.8f, 7.2f, 20.2f, 13.8f)
            lineTo(13.2f, 15.4f)
            close()
            // Boyun: ampulu huniye baglayan egik bant.
            moveTo(7.6f, 13.6f)
            lineTo(11.8f, 10.6f)
            lineTo(13.6f, 13.4f)
            lineTo(9.4f, 16.4f)
            close()
            // Ampul — ikonu MEGAFONDAN ayiran parca.
            addOval(Rect(1.8f, 13.2f, 8.8f, 20.2f))
        }

        // Ses dalgalari: agzin ONUNDE, huni ekseni dogrultusunda iki yay.
        val mouth = Offset(18.6f, 8.8f)
        val waveStroke = 1.6f
        val waveStart = -62f
        val waveSweep = 66f
        val waveRadii = listOf(5.2f, 7.0f)
        val waves = Path().apply {
            waveRadii.forEach { radius ->
                arcTo(
                    rect = Rect(mouth, radius),
                    startAngleDegrees = waveStart,
                    sweepAngleDegrees = waveSweep,
                    forceMoveTo = true
                )
            }
        }

        // Kutuya OTURTMA: sinirlar cizimden turetiliyor, elle ortalama yok.
        // Ilk surumde sabitlerle ortalanmisti ve ikon butonun sag altina
        // kaymisti (2026-08-16, cihazda goruldu).
        //
        // Yaylarin siniri ELDE hesaplaniyor: `Path.getBounds()` bir yay icin
        // yayin degil TAM CEMBERIN kutusunu veriyor, o yuzden ikon gereksiz
        // yere kuculup sola kayiyordu (yine cihazda goruldu). Yay
        // [waveStart]..[waveStart]+[waveSweep] arasinda; bu aralikta x en
        // buyuk 0 derecede, uc noktalar da uclarda.
        val angles = listOf(waveStart, waveStart + waveSweep, 0f)
            .filter { it in waveStart..(waveStart + waveSweep) }
        val rMax = waveRadii.max()
        val waveXs = angles.map { mouth.x + rMax * cos(it * PI.toFloat() / 180f) }
        val waveYs = angles.map { mouth.y + rMax * sin(it * PI.toFloat() / 180f) }

        val b = body.getBounds()
        val m = waveStroke / 2f
        val left = minOf(b.left, waveXs.min() - m)
        val top = minOf(b.top, waveYs.min() - m)
        val right = maxOf(b.right, waveXs.max() + m)
        val bottom = maxOf(b.bottom, waveYs.max() + m)

        // Olcekleme/ortalama diger ikonlarla ayni yerden: [drawFitted].
        drawFitted(Rect(left, top, right, bottom)) {
            drawPath(body, tint)
            drawPath(
                waves,
                tint.copy(alpha = 0.85f),
                style = Stroke(width = waveStroke, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Basili tutuldugu surece aktif olan buton (boost/fren). Icerigi CAGIRAN
 * veriyor: boost cizilmis bir ikon, fren iki dilli bir metin gosteriyor.
 */
@Composable
private fun HoldButton(
    topColor: Color,
    bottomColor: Color,
    modifier: Modifier,
    onHoldChanged: (Boolean) -> Unit,
    content: @Composable () -> Unit
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
        },
        content = content
    )
}

/**
 * Kontrol butonundaki metin (su an yalniz fren). Ikonlarin koyu konturunun
 * metin karsiligi: golge, beyaz harfleri kirmizi zeminden ayiriyor.
 */
@Composable
private fun ControlLabel(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        style = TextStyle(shadow = Shadow(Color(0xB3000000), Offset(0f, 1.5f), 2.5f))
    )
}

// ---------------------------------------------------------------------------
// Overlay'ler
// ---------------------------------------------------------------------------

/**
 * Overlay'lerin ortak zemini: karartma + guvenli alan dolgusu.
 *
 * KAYDIRILABILIR (2026-08-17). Sonuc karti dar ekranda TASIYORDU: kariyerde
 * basarisiz bir kosunun karti (baslik + "N gorev gerekli" satiri + 3 nokta +
 * 7 istatistik satiri + gorev odulu + 3 hedef satiri + reklam butonu +
 * ANA MENU) hesapla ~535 dp; 360x640 dp bir telefonda scrim dolgusu ve sistem
 * cubuklari dusunce ~528 dp kaliyor. Kart ortalandigi icin tasma alttan ve
 * ustten esit kirpiliyor ve EN ALTTAKI "ANA MENU" BUTONU EKRAN DISINDA
 * KALIYORDU (docs/REVIEW_UX.md §1). Yazi tipi olcegi buyutulunce tasma
 * artiyor ve buton tamamen kayboluyor. S8'de (360x740 dp) ~100 dp fazladan
 * yer oldugu icin cihazda bugune kadar gorulmedi.
 *
 * ORTALAMA BOZULMUYOR — sirasi onemli: `fillMaxSize` zincirde
 * `verticalScroll`tan ONCE geliyor, yani kaydirma katmanina SABIT bir
 * yukseklik geliyor ve o da icerige `minHeight = ekran yuksekligi` olarak
 * gecirilir (`Constraints.copy(maxHeight = Infinity)` minHeight'i korur).
 * Icerik kisaysa Box yine tam ekran olur ve `Alignment.Center` aynen calisir;
 * uzunsa Box icerik kadar buyur ve kaydirilir. Kisa overlay'lerin (geri
 * sayim, duraklatma, carpisma) gorunumu bu yuzden degismiyor.
 *
 * Kosu sirasinda maliyeti YOK: bu sarmalayici yalnizca bir overlay aciktayken
 * bestelenir, overlay aciksa da motor zaten durmus/duraklamis durumdadir.
 */
@Composable
private fun OverlayScrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3010610))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * Geri sayim. Ham `Float` yerine GORUNEN saniyeyi alir: motorun sayaci Compose
 * durumu degil, o yuzden "kacinci saniyedeyiz" bilgisi cagri yerinde durum
 * olarak tutuluyor ve yalnizca rakam degistiginde yaziliyor. Gosterilen metin
 * ayni ([kotlin.math.ceil] ile yuvarlama artik cagri yerinde yapiliyor).
 *
 * Isiklar ve geri sayim bipi de AYNI [seconds] degerinden turuyor — yani
 * "bir isik sondu" ile "bip caldi" tek olay, ayri ayri zamanlanmis iki sey
 * degil (bkz. cagri yerindeki not).
 */
@Composable
private fun CountdownOverlay(seconds: Int, language: AppLanguage) {
    OverlayScrim {
        KronCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    language.pick(tr = "HAZIR OL", en = "GET READY"),
                    color = KronColors.Accent,
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                CountdownLights(seconds)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (seconds <= 0) "GO!" else "$seconds",
                    color = KronColors.BlueBright,
                    fontSize = 52.sp
                )
            }
        }
    }
}

/**
 * Geri sayim isiklari — **yanan isik sayisi = kalan rakam**.
 *
 * ```
 * 3  ●●●     2  ●●○     1  ●○○     0 / BASLA  ○○○
 * ```
 *
 * Eslemenin bu kadar duz olmasi kasitli: F1 baslangic isigini bilmeyen
 * oyuncu icin bile kendiliginden anlasilir (isik sayisi zaten rakami
 * soyluyor), bilen icin de son isigin sonmesi kosunun baslamasi demek.
 *
 * ## ⚠ "Hepsi sonuk" karesi CIZILMIYOR — ve bu dogru
 *
 * Yukaridaki tablonun son hucresi (0 → ○○○) bir DURUM olarak vardir ama
 * ekranda gorunmez. Sebep motorun kendisi: `GameEngine.step` sayaci sifira
 * indirdigi KARENIN ICINDE fazi `RUNNING`'e ceviriyor, [CountdownOverlay]
 * ise yalnizca `RunPhase.COUNTDOWN` boyunca ciziliyor. Yani oyuncunun gordugu
 * son geri sayim karesi TEK ISIK, ondan sonraki karede kart tamamen kalkiyor
 * ve oyun basliyor.
 *
 * Deger olarak yine de dogru yazildi ve BOYLE kalmali: sonuc, isiklarin
 * kartla birlikte ayni anda yok olmasi — "lights out and away we go" anini
 * tasiyan sey zaten bu. Isik sayisini bir rakam kaydirip 0 halini gorunur
 * kilmak "3 → iki isik" demek olurdu ve kuralin kendisini bozardi. Gercekten
 * gorunmesi istenirse dogru cozum kartin ~150 ms daha ekranda tutulmasidir,
 * ama o kosunun ilk karelerine perde (`OverlayScrim`) indirir — ayri bir
 * karar, burada verilmedi.
 *
 * ## Uc karar ve gerekceleri
 *
 * **Sonen isik KAYBOLMAZ**, yalnizca kararir. Sonen isik yok edilseydi Row
 * daralir, kalan isiklar ortalanmak icin kayar ve geri sayim her saniye
 * "titriyormus" gibi okunurdu. Yer koruma bu yuzden gorsel degil, okunabilirlik
 * karari.
 *
 * **Renk kirmizi**, sari degil. Kirmizi bu oyunda zaten "dikkat" demek
 * (kerb, carpisma flasi); sari ise boost/enerji rengi
 * ([KronColors.Accent]) ve geri sayimda kullanilsa iki anlam cakisirdi.
 * Yanan isik icin [KronColors.PlayerRed] — oyuncunun kendi araci da o
 * kirmizi. Sonuk hal [KronColors.Locked]: katalogda "henuz acilmadi"
 * anlamini tasiyan koyu lacivert, burada da "bu isik artik yok" diyor.
 *
 * **Golge/bulaniklik YOK.** Cihaz S8 (API 24) ve oyun ~40 FPS'te; `blur`
 * burada pahali. "Parliyor" hissini bunun yerine ince bir halka tasiyor:
 * yanan isik ampulden acik kirmizi bir cerceve, sonuk isik neredeyse
 * gorunmez bir ic hat aliyor. Cizim uc `Box` — sahne zaten yalnizca geri
 * sayim boyunca duruyor, ama ucuz olmasinin da bir bedeli yok.
 *
 * Metin icermez, bu yuzden [AppLanguage] almaz.
 */
@Composable
private fun CountdownLights(litCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(COUNTDOWN_LIGHT_GAP)) {
        repeat(COUNTDOWN_LIGHT_COUNT) { index ->
            // Sondurme SAGDAN sola ilerler (index 2, sonra 1, sonra 0):
            // yakit/sarj gostergesi gibi "kalan miktar" okunur.
            val lit = index < litCount
            Box(
                modifier = Modifier
                    .size(COUNTDOWN_LIGHT_SIZE)
                    .clip(CircleShape)
                    .background(if (lit) KronColors.PlayerRed else KronColors.Locked)
                    .border(
                        width = COUNTDOWN_LIGHT_RING,
                        color = if (lit) COUNTDOWN_LIGHT_RING_ON else COUNTDOWN_LIGHT_RING_OFF,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Isik sayisi geri sayimin kendisinden TURETILIR, elle yazilmaz: kural
 * "yanan isik sayisi = kalan rakam" oldugu icin ikisi ayri yerlerde
 * tutulsaydi biri degistiginde sessizce yalan soylerlerdi (ornegin sayac
 * 5'e cikarilir, isiklar 3'te kalir ve geri sayim iki saniye boyunca
 * "3 isik" gosterirdi).
 */
private const val COUNTDOWN_LIGHT_COUNT = GameConfig.COUNTDOWN_SECONDS
private val COUNTDOWN_LIGHT_SIZE = 22.dp
private val COUNTDOWN_LIGHT_GAP = 12.dp
private val COUNTDOWN_LIGHT_RING = 2.dp

/** Yanan isigin sicak kenari — ampulun kendisinden acik, "parliyor" hissi. */
private val COUNTDOWN_LIGHT_RING_ON = Color(0xFFFF8A80)

/** Sonuk isigin ic hatti: yerini belli eder, dikkat cekmez. */
private val COUNTDOWN_LIGHT_RING_OFF = Color(0x33FFFFFF)

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
    /** Reklam yuklenemediyse butonun altinda tek cumlelik aciklama cikar. */
    adFailed: Boolean,
    onDoubleCoins: () -> Unit,
    onNext: () -> Unit,
    onHome: () -> Unit
) {
    val stats = result.stats
    OverlayScrim {
        KronCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Kariyerde "TAMAMLANDI" ancak bolum GECILDIYSE yazar
                // (bkz. RunResult.passed / GameConfig.MIN_STARS_TO_PASS).
                // Baslik bunu durustce soylemeli, aksi halde oyuncu bolumun
                // acildigini sanip haritada kilitli buluyordu.
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
                        // Metin sabiti DEGIL, sayidan turetiliyor: esik
                        // degisirse ekran sessizce yalan soylemesin.
                        text = language.pick(
                            tr = "Bölümü geçmek için ${GameConfig.MIN_STARS_TO_PASS} görev gerekli.",
                            en = "${GameConfig.MIN_STARS_TO_PASS} objectives are required to pass."
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

                        else -> {
                            PrimaryButton(
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
                            // Metin ve yerlesim CrashOverlay'dekiyle BIREBIR
                            // ayni (docs/REVIEW_UX.md §4: "cozum zaten kodda
                            // var, ayni desen tasinmali") — oyuncu ayni hatayi
                            // iki ekranda iki farkli cumleyle okumasin.
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
                        }
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
