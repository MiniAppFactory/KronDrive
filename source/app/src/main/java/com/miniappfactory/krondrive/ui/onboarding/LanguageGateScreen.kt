package com.miniappfactory.krondrive.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniappfactory.krondrive.R
import com.miniappfactory.krondrive.data.AppLanguage
import com.miniappfactory.krondrive.game.CarCatalog
import com.miniappfactory.krondrive.game.GameConfig
import com.miniappfactory.krondrive.game.GameEngine
import com.miniappfactory.krondrive.ui.common.CarSpriteSet
import com.miniappfactory.krondrive.ui.common.rememberCarSprites
import com.miniappfactory.krondrive.ui.common.drawStyledCar
import com.miniappfactory.krondrive.ui.theme.KronColors
import kotlin.math.min

/**
 * Ilk acilis dil ekrani (sahibi istegi, 2026-08-15).
 *
 * Dil cihazin yerel ayarindan TAHMIN ediliyordu; bu ekran tahmini oyuncuya
 * onaylatiyor. Yalnizca bir kez cikar
 * ([com.miniappfactory.krondrive.data.PlayerProgress.languageChosen]).
 *
 * Zemin duz siyah DEGIL: sahibi "siyah ekran kasvetli bir giris oluyor,
 * insanlar oyuna girdiklerini hissetsin" dedi. Bu yuzden arkada oyunun kendi
 * gece pisti duruyor — yol, kerb, serit cizgileri ve uc arac oyunun GERCEK
 * cizim koduyla ([drawStyledCar]) ciziliyor. Boylece:
 *   - Araclar serit merkezlerine oturur, "yoldan kaymis" gorunemez.
 *   - Arac cizimi guncellendiginde bu ekran da kendiliginden guncellenir.
 *   - APK'ya tek bir ek PNG bile girmez.
 *
 * Metinler KASITLI olarak iki dilde birden: dili henuz secmemis oyuncuya tek
 * dilde soru sormak, yanlis dili konusma riski demek.
 *
 * Banner reklam yok — consent akisi cozulmeden reklam gosterilmez, ustelik
 * burasi uygulamanin ilk izlenimi.
 */
@Composable
fun LanguageGateScreen(onChoose: (AppLanguage) -> Unit) {
    val density = LocalDensity.current.density
    val carSprites = rememberCarSprites()

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawNightTrackBackdrop(density, carSprites)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Image(
                painter = painterResource(R.drawable.kron_logo),
                contentDescription = null,
                modifier = Modifier.size(116.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "KRON DRIVE",
                color = KronColors.Accent,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "DRIVE · DODGE · SURVIVE",
                color = KronColors.TextMuted,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(2.dp)
                    .background(KronColors.BlueBright)
            )

            Spacer(modifier = Modifier.height(38.dp))

            Text(
                text = "Dil seçin  ·  Choose language",
                color = KronColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Iki dil YAN YANA ve esit agirlikta. Biri dolu digeri cerceveli
            // olsaydi "onerilen dil" sinyali verirdi; hangi dilin onerildigini
            // bilemeyecegimiz icin ikisi de ayni.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LanguagePill(
                    label = "TÜRKÇE",
                    modifier = Modifier.weight(1f),
                    onClick = { onChoose(AppLanguage.TR) }
                )
                LanguagePill(
                    label = "ENGLISH",
                    modifier = Modifier.weight(1f),
                    onClick = { onChoose(AppLanguage.EN) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Ayarlardan değiştirebilirsin · You can change this in Settings",
                color = KronColors.TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Hap seklinde, sari dolgulu dil butonu. */
@Composable
private fun LanguagePill(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .background(
                Brush.verticalGradient(listOf(KronColors.AccentBright, KronColors.Accent)),
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = KronColors.Background,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * Gece pisti zemini: gradyan gokyuzu, uzak sehir isiklari, yol, kerb, serit
 * cizgileri ve uc arac. Olculer oyunun kendi sabitlerinden turetiliyor
 * ([GameConfig]), yani yol genisligi ve desen frekansi oyundakiyle ayni.
 *
 * Ustteki %58'lik bant koyu bir perde ile kapatiliyor: yazilar her cihazda
 * okunsun, alttaki sahne de gorunur kalsin.
 */
private fun DrawScope.drawNightTrackBackdrop(density: Float, sprites: CarSpriteSet) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    // Gokyuzu
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF07111F), Color(0xFF0F2138))),
        Offset.Zero,
        Size(w, h)
    )

    // Uzak sehir isiklari (ust yariya dagilmis, sabit desen)
    var seed = 7
    repeat(70) {
        seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
        val x = (seed % w.toInt()).toFloat()
        seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
        val y = (seed % (h * 0.55f).toInt()).toFloat()
        val warm = (seed / 7) % 2 == 0
        drawRect(
            if (warm) Color(0x99FFD54F) else Color(0x9990CAF9),
            Offset(x, y),
            Size(3f * density, 8f * density)
        )
    }

    // Yol — oyundaki ile ayni oran
    val roadWidth = min(GameConfig.ROAD_MAX_WIDTH_PX * density, w * GameConfig.ROAD_WIDTH_RATIO)
    val roadX = (w - roadWidth) / 2f
    drawRect(Color(0xFF3A4048), Offset(roadX, 0f), Size(roadWidth, h))

    // Kerb — frekans ve renkler oyundakiyle ayni (goz yorgunlugu ayari)
    val block = GameConfig.KERB_BLOCK_HEIGHT_PX * density
    var y = 0f
    var index = 0
    while (y < h) {
        val color = if (index % 2 == 0) Color(0xFFC8393B) else Color(0xFFDCE2E9)
        drawRect(color, Offset(roadX - 8f * density, y), Size(8f * density, block))
        drawRect(color, Offset(roadX + roadWidth, y), Size(8f * density, block))
        y += block
        index++
    }

    // Serit cizgileri
    val on = GameConfig.LANE_DASH_ON_PX * density
    val off = GameConfig.LANE_DASH_OFF_PX * density
    val dash = PathEffect.dashPathEffect(floatArrayOf(on, off), 0f)
    val laneWidth = roadWidth / GameConfig.LANE_COUNT
    for (i in 1 until GameConfig.LANE_COUNT) {
        val x = roadX + laneWidth * i
        drawLine(
            color = Color(0xC2FFFFFF),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 5f * density,
            pathEffect = dash
        )
    }

    // Araclar: serit MERKEZLERINE oturur (oyundaki laneCenter ile ayni hesap).
    fun laneCenter(lane: Int) = roadX + laneWidth * lane + laneWidth / 2f
    val carScale = density * 1.35f
    val playerY = h - 78f * carScale
    val trafficY = h - 118f * carScale

    scale(carScale, carScale, pivot = Offset.Zero) {
        drawStyledCar(
            x = laneCenter(0) / carScale,
            y = trafficY / carScale,
            style = CarCatalog.trafficStyle(GameEngine.OBSTACLE_COLORS[0].toLong() and 0xFFFFFFFFL),
            boosting = false,
            sprites = sprites
        )
        drawStyledCar(
            x = laneCenter(2) / carScale,
            y = (trafficY - 26f * carScale) / carScale,
            style = CarCatalog.trafficStyle(GameEngine.OBSTACLE_COLORS[1].toLong() and 0xFFFFFFFFL),
            boosting = false,
            sprites = sprites
        )
        drawStyledCar(
            x = laneCenter(1) / carScale,
            y = playerY / carScale,
            style = CarCatalog.defaultStyle,
            boosting = true,
            sprites = sprites
        )
    }

    // Metin bandi icin perde: ustte koyu, ortada sonumleniyor.
    drawRect(
        Brush.verticalGradient(
            0f to Color(0xF2020915),
            0.52f to Color(0xE6020915),
            0.72f to Color(0x66020915),
            1f to Color(0x33020915)
        ),
        Offset.Zero,
        Size(w, h)
    )
}
