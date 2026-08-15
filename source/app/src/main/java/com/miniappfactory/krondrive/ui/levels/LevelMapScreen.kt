package com.miniappfactory.krondrive.ui.levels

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniappfactory.krondrive.data.AppLanguage
import com.miniappfactory.krondrive.data.BoosterType
import com.miniappfactory.krondrive.data.PlayerProgress
import com.miniappfactory.krondrive.game.CarStyle
import com.miniappfactory.krondrive.game.LevelCatalog
import com.miniappfactory.krondrive.game.LevelDef
import com.miniappfactory.krondrive.game.LevelGoal
import com.miniappfactory.krondrive.ui.common.CarPreview
import com.miniappfactory.krondrive.ui.common.KronScreen
import com.miniappfactory.krondrive.ui.common.ObjectiveDots
import com.miniappfactory.krondrive.ui.common.PrimaryButton
import com.miniappfactory.krondrive.ui.common.dimmed
import com.miniappfactory.krondrive.ui.theme.KronColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Kariyer haritasi — kerbli, kivrimli bir YARIS PISTI.
 *
 * Sahibi istegi (2026-08-15): "Bolum haritasini kerbli kavisli bir pist gibi
 * yapip uzerinde bir araba olsa, levelleri gectikce hareket eden". Eski 4'lu
 * izgara kaldirildi; bolumler artik pistin uzerinde sirayla dizili DURAKLAR.
 *
 * Ekranin uc parcasi var:
 *  1. [LazyColumn] — her bolum icin bir segment. Sanallastirildigi icin 30
 *     bolumluk pistte de yalnizca gorunen 4-5 segment bestelenir; pist yollari
 *     ise [buildTrackSegmentArt] ile BIR KEZ kurulup paylasilir.
 *  2. Duraklar — durum (kilitli / oynanabilir / tamamlanmis) daireden okunur,
 *     gorev sayaci [ObjectiveDots] ile yanda durur. Yildiz dili YOK
 *     (sahibi karari): tamamlanan bolumde damali bayrak ve yesil tikler var.
 *  3. Oyuncunun garajdaki araci — pistin uzerinde, bulundugu duragin hemen
 *     gerisinde park eder; bolum acildiginda bir sonraki duraga KAYARAK gider.
 *
 * Geometri bu dosyada DEGIL, [TrackLayout] icinde (saf Kotlin + JVM testleri).
 *
 * Booster secimi hala bolum kartinda (garajda degil): oyuncu hedefi gorduk-
 * ten sonra hangi guclendiriciyi harcayacagina karar verebilsin.
 */
@Composable
fun LevelMapScreen(
    progress: PlayerProgress,
    selectedBoosters: Set<BoosterType>,
    adsConsentResolved: Boolean,
    onToggleBooster: (BoosterType) -> Unit,
    onPlayLevel: (Int) -> Unit,
    onBack: () -> Unit
) {
    val language = progress.language
    val levels = LevelCatalog.levels
    val currentLevel = progress.highestUnlockedLevel.coerceIn(1, levels.size)
    var selected by remember { mutableStateOf<LevelDef?>(null) }

    KronScreen(
        title = language.pick(tr = "KARİYER", en = "CAREER"),
        onBack = onBack,
        showBanner = adsConsentResolved
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().clipToBounds()) {
            val widthDp = maxWidth.value
            val density = LocalDensity.current.density
            val layout = remember(widthDp) { TrackLayout(stopCount = levels.size, width = widthDp) }
            // Agir is: 30 segmentin yollari. Genislik degismedikce kurulmaz.
            val art = remember(layout, density) { buildTrackSegmentArt(layout, density) }
            val listState = rememberLazyListState()

            // Aracin O ANDA durdugu bolum. `rememberSaveable` sayesinde oyundan
            // geri donuldugunde eski deger geliyor; boylece "bir bolum ilerledi"
            // animasyonu ekran yeniden kurulsa da oynatilabiliyor.
            var shownLevel by rememberSaveable { mutableIntStateOf(currentLevel) }
            val carPosition = remember { Animatable((shownLevel - 1).toFloat()) }

            // Oyuncu haritayi actiginda KENDI bolumunu gorsun: bir ustteki
            // segment ekranin tepesine getiriliyor.
            //
            // [adsConsentResolved] anahtar olarak da veriliyor: banner reklam
            // SONRADAN yukleniyor, yuklendigi an icerik alani kisaliyor ve
            // liste ayni kaydirma konumunda kaldigi icin en alttaki durak
            // (bolum 1 ve uzerindeki arac) banner'in arkasinda kaliyordu
            // (oyuncu bildirimi, 2026-08-15). Banner gelince konum yeniden
            // hesaplaniyor.
            LaunchedEffect(adsConsentResolved) {
                val segment = layout.segmentOfLevel(shownLevel - 1)
                listState.scrollToItem((segment - 1).coerceAtLeast(0))
            }

            LaunchedEffect(currentLevel) {
                if (currentLevel != shownLevel) {
                    delay(CAR_MOVE_DELAY_MS)
                    val segment = layout.segmentOfLevel(currentLevel - 1)
                    launch { listState.animateScrollToItem((segment - 1).coerceAtLeast(0)) }
                    carPosition.animateTo(
                        targetValue = (currentLevel - 1).toFloat(),
                        animationSpec = tween(CAR_MOVE_MS, easing = FastOutSlowInEasing)
                    )
                    shownLevel = currentLevel
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // Alttaki durak banner'a yapisik durmasin; arac duragin
                // gerisinde park ettigi icin ona da yer birakiyor.
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(count = levels.size, key = { it }) { segmentIndex ->
                    val level = levels[layout.levelOfSegment(segmentIndex)]
                    TrackSegmentRow(
                        layout = layout,
                        art = art,
                        density = density,
                        segmentIndex = segmentIndex,
                        level = level,
                        earned = progress.starsOf(level.id),
                        locked = level.id > progress.highestUnlockedLevel,
                        isCurrent = level.id == currentLevel,
                        language = language,
                        onClick = { selected = level }
                    )
                }
            }

            PlayerCarOnTrack(
                layout = layout,
                style = progress.carStyle,
                density = density,
                scrollPx = {
                    listState.firstVisibleItemIndex * layout.segmentHeight * density +
                        listState.firstVisibleItemScrollOffset
                },
                position = { carPosition.value }
            )
        }
    }

    val detail = selected
    if (detail != null) {
        LevelDetailDialog(
            level = detail,
            earnedObjectives = progress.starsOf(detail.id),
            progress = progress,
            selectedBoosters = selectedBoosters,
            onToggleBooster = onToggleBooster,
            onDismiss = { selected = null },
            onPlay = {
                selected = null
                onPlayLevel(detail.id)
            }
        )
    }
}

/** Aracin bir sonraki duraga kaymasi — sonuc ekrani kapandiktan sonra. */
private const val CAR_MOVE_DELAY_MS = 350L
private const val CAR_MOVE_MS = 1100

/** Yan bilgi paneli ile ekran kenari arasindaki bosluk (dp). */
private const val PANEL_EDGE = 8f

/** Panelin yola en yakin mesafesi (dp). */
private const val PANEL_GAP = 12f

/** Panel bu genisligin altina duserse hic cizilmez (cok dar ekran). */
private const val PANEL_MIN_WIDTH = 74f

// ---------------------------------------------------------------------------
// Pist
// ---------------------------------------------------------------------------

/** Bir bolume ayrilan dilim: pistin o parcasi + durak + yan bilgi. */
@Composable
private fun TrackSegmentRow(
    layout: TrackLayout,
    art: TrackSegmentArt,
    density: Float,
    segmentIndex: Int,
    level: LevelDef,
    earned: Int,
    locked: Boolean,
    isCurrent: Boolean,
    language: AppLanguage,
    onClick: () -> Unit
) {
    // Durak her zaman segmentin tam ortasinda (bkz. TrackLayout.stopY).
    val stopLocalY = layout.segmentHeight / 2f
    val stopX = layout.localCenterX(stopLocalY, segmentIndex)
    val panelOnLeft = stopX > layout.width / 2f

    // Pano yolun BOS tarafinda durur. Yol, duragin ustunde ve altinda panoya
    // dogru kivriliyor (durak sinusun tepesi) — bu yuzden sinir tek noktadan
    // degil, PANONUN TUM YUKSEKLIGINDEN okunuyor. Aksi halde "HAYATTA KAL"
    // basligi asfaltin uzerine biniyordu (mockup'ta yakalandi).
    val side = if (panelOnLeft) -layout.halfWidthWithKerb else layout.halfWidthWithKerb
    val roadBound = (-2..2).map { step ->
        val y = stopLocalY + step * PANEL_HALF_HEIGHT / 2f
        layout.localEdgePoint(y, side, segmentIndex).x
    }.let { if (panelOnLeft) it.min() else it.max() }

    val panelX = if (panelOnLeft) PANEL_EDGE else roadBound + PANEL_GAP
    val panelWidth = if (panelOnLeft) {
        roadBound - PANEL_GAP - PANEL_EDGE
    } else {
        layout.width - roadBound - PANEL_GAP - PANEL_EDGE
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(layout.segmentHeight.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTrackSegment(art, segmentIndex)
            // Pistin iki ucu: baslangic ve bitis cizgisi.
            if (level.id == 1) {
                drawCheckerBand(layout, density, segmentIndex, layout.segmentHeight * 0.80f)
            }
            if (level.id == layout.stopCount) {
                drawCheckerBand(layout, density, segmentIndex, layout.segmentHeight * 0.12f)
            }
        }

        if (panelWidth >= PANEL_MIN_WIDTH) {
            StopPanel(
                level = level,
                earned = earned,
                locked = locked,
                alignEnd = panelOnLeft,
                language = language,
                modifier = Modifier
                    .offset(x = panelX.dp, y = (stopLocalY - PANEL_HALF_HEIGHT).dp)
                    .width(panelWidth.dp),
                onClick = onClick
            )
        }

        StopMarker(
            level = level,
            earned = earned,
            locked = locked,
            isCurrent = isCurrent,
            language = language,
            modifier = Modifier.offset(
                x = (stopX - TrackLayout.STOP_DIAMETER / 2f).dp,
                y = (stopLocalY - TrackLayout.STOP_DIAMETER / 2f).dp
            ),
            onClick = onClick
        )
    }
}

private const val PANEL_HALF_HEIGHT = 26f

/**
 * Duragin kendisi. Dokunma hedefi [TrackLayout.STOP_DIAMETER] = 56 dp, yani
 * Material'in 48 dp alt sinirinin uzerinde.
 */
@Composable
private fun StopMarker(
    level: LevelDef,
    earned: Int,
    locked: Boolean,
    isCurrent: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val completed = earned > 0
    val ring = when {
        locked -> KronColors.SurfaceBorder
        isCurrent -> KronColors.AccentBright
        completed -> KronColors.ObjectiveDone
        else -> KronColors.Accent.copy(alpha = 0.55f)
    }
    val fill = when {
        locked -> KronColors.Locked
        isCurrent -> KronColors.Accent
        else -> KronColors.SurfaceDeep
    }

    Box(
        modifier = modifier
            .size(TrackLayout.STOP_DIAMETER.dp)
            .dimmed(locked)
            .clickable(enabled = !locked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            // Disardaki yumusak halka: durak asfaltin uzerinde yuzuyor gibi.
            drawCircle(ring.copy(alpha = if (isCurrent) 0.34f else 0.16f), radius)
            drawCircle(fill, radius - 5f * density)
            drawCircle(ring, radius - 5f * density, style = Stroke(width = 3f * density))
        }

        if (locked) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = language.pick(tr = "Kilitli", en = "Locked"),
                tint = KronColors.TextMuted,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = "${level.id}",
                color = if (isCurrent) KronColors.Background else KronColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }

        if (completed) {
            CheckeredFlag(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-3).dp)
                    .size(width = 18.dp, height = 16.dp)
            )
        }
    }
}

/** Tamamlanmis bolumun damali bayragi (Canvas — ek PNG yok). */
@Composable
private fun CheckeredFlag(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val pole = 1.6f * density
        drawRect(
            color = KronColors.TextSecondary,
            topLeft = Offset.Zero,
            size = Size(pole, size.height)
        )
        val cols = 3
        val rows = 2
        val cell = (size.width - pole) / cols
        for (row in 0 until rows) {
            for (column in 0 until cols) {
                drawRect(
                    color = if ((row + column) % 2 == 0) {
                        KronColors.TextPrimary
                    } else {
                        Color(0xFF10151F)
                    },
                    topLeft = Offset(pole + column * cell, row * cell),
                    size = Size(cell, cell)
                )
            }
        }
        drawRect(
            color = Color(0x33000000),
            topLeft = Offset(pole, 0f),
            size = Size(cell * cols, cell * rows),
            style = Stroke(width = density)
        )
    }
}

/**
 * Duragin yanindaki kucuk pano: bolumun hedefi ve gorev sayaci. Pano her
 * zaman yolun BOS tarafinda durur (durak sagdaysa pano solda), boylece 360 dp
 * genislikte bile yazi asfaltin uzerine binmez.
 */
@Composable
private fun StopPanel(
    level: LevelDef,
    earned: Int,
    locked: Boolean,
    alignEnd: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .dimmed(locked)
            .clickable(enabled = !locked, onClick = onClick),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        // Ellipsis sart: TR basliklar EN'den uzun ve pano genisligi sabit.
        Text(
            text = goalCaption(level.goal, language),
            color = KronColors.TextMuted,
            fontSize = 10.sp,
            letterSpacing = 1.1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = goalValue(level.goal, language),
            color = if (locked) KronColors.TextSecondary else KronColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!locked) {
            ObjectiveDots(
                earned = earned,
                dotSize = 13,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Oyuncunun garajda sectigi arac, pistin uzerinde.
 *
 * Konum ve aci [Modifier.offset]/[graphicsLayer] LAMBDALARINDA okunuyor: hem
 * kaydirma hem de ilerleme animasyonu boylece yeniden besteleme yapmadan,
 * sadece yerlesim/cizim asamasinda calisiyor.
 *
 * Arac duragin tam uzerine degil, [TrackLayout.CAR_TRAIL] kadar GERISINE
 * park eder — bolum numarasi aracin altinda kaybolmasin diye.
 */
@Composable
private fun PlayerCarOnTrack(
    layout: TrackLayout,
    style: CarStyle,
    density: Float,
    scrollPx: () -> Float,
    position: () -> Float
) {
    CarPreview(
        style = style,
        modifier = Modifier
            .offset {
                val point = layout.pointAt(position() - TrackLayout.CAR_TRAIL)
                IntOffset(
                    x = ((point.x - TrackLayout.CAR_WIDTH / 2f) * density).roundToInt(),
                    y = ((point.y - TrackLayout.CAR_HEIGHT / 2f) * density - scrollPx())
                        .roundToInt()
                )
            }
            .size(width = TrackLayout.CAR_WIDTH.dp, height = TrackLayout.CAR_HEIGHT.dp)
            .graphicsLayer {
                rotationZ = layout.headingDegreesAt(position() - TrackLayout.CAR_TRAIL)
            }
    )
}

// ---------------------------------------------------------------------------
// Bolum karti
// ---------------------------------------------------------------------------

/**
 * Bolum detayi. AlertDialog secildi cunku geri tusu/disariya dokunma ile
 * kapanmayi kendisi hallediyor — ayri bir "kapat" durumu tutmaya gerek kalmiyor.
 */
@Composable
private fun LevelDetailDialog(
    level: LevelDef,
    earnedObjectives: Int,
    progress: PlayerProgress,
    selectedBoosters: Set<BoosterType>,
    onToggleBooster: (BoosterType) -> Unit,
    onDismiss: () -> Unit,
    onPlay: () -> Unit
) {
    val language = progress.language

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = KronColors.Surface,
        titleContentColor = KronColors.Accent,
        textContentColor = KronColors.TextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = language.pick(tr = "Bölüm ${level.id}", en = "Level ${level.id}"),
                    color = KronColors.Accent,
                    fontSize = 22.sp
                )
                ObjectiveDots(earned = earnedObjectives, dotSize = 14)
            }
        },
        text = {
            Column(
                // Uzun hedef listesi + booster izgarasi 360x640'ta tasabilir.
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = goalText(level.goal, language),
                    color = KronColors.TextPrimary,
                    fontSize = 14.sp
                )

                Text(
                    text = language.pick(tr = "BÖLÜM GÖREVLERİ", en = "LEVEL OBJECTIVES"),
                    color = KronColors.TextMuted,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp
                )

                level.stars.forEachIndexed { index, objective ->
                    val done = index < earnedObjectives
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Yildiz degil, gorev isareti: [ObjectiveDots] ile ayni dil.
                        ObjectiveBullet(done = done)
                        Text(
                            text = objective.label(language),
                            color = if (done) KronColors.TextPrimary else KronColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Text(
                    text = language.pick(tr = "GÜÇLENDİRİCİLER", en = "BOOSTERS"),
                    color = KronColors.TextMuted,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // 4 booster tek satira sigmaz (uzun isimler); 2x2 duzen kullaniliyor.
                BoosterType.entries.chunked(2).forEach { rowTypes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTypes.forEach { type ->
                            BoosterChip(
                                type = type,
                                owned = progress.boosterCount(type),
                                selected = type in selectedBoosters,
                                language = language,
                                modifier = Modifier.weight(1f),
                                onClick = { onToggleBooster(type) }
                            )
                        }
                        // Tek elemanli son satirda hizalama bozulmasin.
                        if (rowTypes.size == 1) Column(modifier = Modifier.weight(1f)) {}
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = language.pick(tr = "BAŞLA", en = "START"),
                modifier = Modifier.fillMaxWidth(),
                onClick = onPlay
            )
        }
    )
}

/** Tek bir gorev isareti — [ObjectiveDots] ile ayni gorsel dil. */
@Composable
private fun ObjectiveBullet(done: Boolean, size: Int = 16) {
    Box(modifier = Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        if (done) {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .background(KronColors.ObjectiveDone, CircleShape)
            )
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = KronColors.Background,
                modifier = Modifier.size((size * 0.72f).dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .border((size * 0.1f).dp, KronColors.TextMuted, CircleShape)
            )
        }
    }
}

@Composable
private fun BoosterChip(
    type: BoosterType,
    owned: Int,
    selected: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val available = owned > 0
    Column(
        modifier = modifier
            .dimmed(!available)
            .heightIn(min = 48.dp)
            .background(
                if (selected) KronColors.Blue.copy(alpha = 0.18f) else KronColors.SurfaceDeep,
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                if (selected) KronColors.BlueBright else KronColors.SurfaceBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = available, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = type.title(language),
            color = if (selected) KronColors.BlueBright else KronColors.TextPrimary,
            fontSize = 12.sp,
            maxLines = 2
        )
        Text(
            text = if (available) {
                language.pick(tr = "Elinde: $owned", en = "Owned: $owned")
            } else {
                language.pick(tr = "Yok", en = "None")
            },
            color = KronColors.TextMuted,
            fontSize = 10.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Metinler
// ---------------------------------------------------------------------------

/** Yan panonun ust satiri — hedefin TURU. */
private fun goalCaption(goal: LevelGoal, language: AppLanguage): String = when (goal) {
    is LevelGoal.SurviveTime -> language.pick(tr = "HAYATTA KAL", en = "SURVIVE")
    is LevelGoal.ReachDistance -> language.pick(tr = "MESAFE", en = "DISTANCE")
}

/** Yan panonun buyuk satiri — hedefin DEGERI. Dar panoda tek satir kalmali. */
private fun goalValue(goal: LevelGoal, language: AppLanguage): String = when (goal) {
    is LevelGoal.SurviveTime -> language.pick(tr = "${goal.seconds} sn", en = "${goal.seconds}s")
    is LevelGoal.ReachDistance -> "${goal.meters} m"
}

/** Bolumun bitis kosulu — motor bunu [LevelGoal] olarak biliyor, oyuncu metin olarak. */
private fun goalText(goal: LevelGoal, language: AppLanguage): String = when (goal) {
    is LevelGoal.SurviveTime -> language.pick(
        tr = "${goal.seconds} saniye hayatta kal",
        en = "Survive ${goal.seconds} seconds"
    )

    is LevelGoal.ReachDistance -> language.pick(
        tr = "${goal.meters} m git (${goal.timeLimitSec} sn limit)",
        en = "Reach ${goal.meters} m (${goal.timeLimitSec}s limit)"
    )
}
