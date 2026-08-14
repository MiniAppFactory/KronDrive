package com.miniappfactory.krondrive.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniappfactory.krondrive.ads.BannerAdView
import com.miniappfactory.krondrive.ui.theme.KronColors

/**
 * Ekranlarin ortak iskeleti: prototipteki radial gradient zemin, ustte baslik
 * satiri, altta (istege bagli) banner reklam.
 *
 * Banner SADECE oyun disi ekranlarda ve consent cozulduginde gosterilir.
 */
@Composable
fun KronScreen(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    showBanner: Boolean = false,
    headerContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(KronColors.BackgroundTop, KronColors.BackgroundMid, KronColors.Background)
                )
            )
    ) {
        // Icerik sistem cubuklarinin (durum cubugu, gezinme cubugu, centik)
        // ARKASINDA kalmasin diye — zemin gradyani ise tum ekrani kaplar.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            if (title != null || onBack != null || headerContent != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(KronColors.Surface, RoundedCornerShape(14.dp))
                                .border(1.dp, KronColors.SurfaceBorder, RoundedCornerShape(14.dp))
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = KronColors.Accent
                            )
                        }
                    }
                    if (title != null) {
                        Text(
                            text = title,
                            color = KronColors.Accent,
                            fontSize = 22.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                    headerContent?.invoke()
                }
            }

            Box(modifier = Modifier.weight(1f)) { content() }

            if (showBanner) {
                BannerAdView(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Prototipteki .menu-card / .result-card karsiligi. */
@Composable
fun KronCard(
    modifier: Modifier = Modifier,
    borderColor: Color = KronColors.SurfaceBorder,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(KronColors.Surface, RoundedCornerShape(22.dp))
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) { content() }
}

/** Sari dolgulu ana buton (.btn.primary). */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(
                if (enabled) {
                    Brush.verticalGradient(listOf(KronColors.AccentBright, KronColors.Accent))
                } else {
                    Brush.verticalGradient(listOf(KronColors.Locked, KronColors.Locked))
                },
                RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = if (enabled) KronColors.Background else KronColors.TextMuted,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = if (enabled) KronColors.Background.copy(alpha = 0.7f) else KronColors.TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Cerceveli ikincil buton (.btn.secondary). */
@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color.Transparent, RoundedCornerShape(18.dp))
            .border(
                2.dp,
                if (enabled) KronColors.Accent.copy(alpha = 0.4f) else KronColors.SurfaceBorder,
                RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) KronColors.Accent else KronColors.TextMuted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** ★☆☆ / ★★☆ / ★★★ gosterimi. */
@Composable
fun StarRow(
    earned: Int,
    total: Int = 3,
    starSize: Int = 18,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(total) { index ->
            if (index < earned) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = KronColors.Accent,
                    modifier = Modifier.size(starSize.dp)
                )
            } else {
                Icon(
                    Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = KronColors.TextMuted,
                    modifier = Modifier.size(starSize.dp)
                )
            }
        }
    }
}

/** Coin/yildiz gibi ust satir rozetleri. */
@Composable
fun StatChip(
    label: String,
    value: String,
    tint: Color = KronColors.Accent,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(KronColors.SurfaceDeep, RoundedCornerShape(14.dp))
            .border(1.dp, KronColors.SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = label, color = tint, fontSize = 12.sp)
        Text(text = value, color = KronColors.TextPrimary, fontSize = 13.sp)
    }
}

@Composable
fun KronProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = KronColors.Blue
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp),
        color = color,
        trackColor = KronColors.Locked,
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}

/** Kilitli/pasif icerikleri soluklastirmak icin. */
fun Modifier.dimmed(dim: Boolean): Modifier = if (dim) this.alpha(0.45f) else this
