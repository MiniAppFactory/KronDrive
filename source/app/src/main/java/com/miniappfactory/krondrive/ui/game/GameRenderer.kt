package com.miniappfactory.krondrive.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.miniappfactory.krondrive.game.CarCatalog
import com.miniappfactory.krondrive.game.GameConfig
import com.miniappfactory.krondrive.game.GameEngine
import com.miniappfactory.krondrive.game.RoadTheme
import com.miniappfactory.krondrive.ui.common.CarSpriteSet
import com.miniappfactory.krondrive.ui.common.drawCarBody
import com.miniappfactory.krondrive.ui.common.drawCarShadow
import com.miniappfactory.krondrive.ui.common.drawStyledCar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Sahnenin cizimi. Prototipteki `drawSideBackgrounds` / `drawTrack` /
 * `drawSpeedometer` / `drawCar` / `drawCoin` / `drawParticles` fonksiyonlarinin
 * BIREBIR karsiligi — ayni koordinatlar, ayni renkler, ayni katman sirasi.
 *
 * Koordinat sistemi: motor "dp uzayinda" calisir (HTML'in CSS pikseli ile ayni
 * olcek). Cizerken tum sahne [density] ile olceklenir; boylece oyun her ekran
 * yogunlugunda AYNI buyuklukte gorunur — ham piksel kullansaydik yuksek DPI
 * telefonlarda yol ve araclar minicik kalirdi.
 */
fun DrawScope.drawGameScene(
    engine: GameEngine,
    density: Float,
    textMeasurer: TextMeasurer,
    gaugeValueSize: TextUnit,
    gaugeLabelSize: TextUnit,
    gaugeSmallSize: TextUnit,
    // Sprite kumesi disaridan gelir: yukleme Composable bir istir, cizim ise
    // her karede yurur. null verilirse cizim vektor yoluna duser.
    sprites: CarSpriteSet? = null
) {
    scale(density, density, pivot = Offset.Zero) {
        drawSideBackgrounds(engine)
        drawTrack(engine)
    }
    drawSpeedometer(engine, density, textMeasurer, gaugeValueSize, gaugeLabelSize, gaugeSmallSize)
    scale(density, density, pivot = Offset.Zero) {
        engine.coins.forEach { drawCoin(it) }
        engine.obstacles.forEach {
            drawObstacleCar(
                it.x,
                it.y,
                GameEngine.OBSTACLE_COLORS[it.colorIndex].toLong() and 0xFFFFFFFFL,
                sprites
            )
        }
        drawNightHeadlights(engine)
        // Dokunulmazlik sirasinda arac yanip soner (Second Chance / reklamla devam).
        val blink = engine.isInvulnerable() && ((engine.timeElapsed * 10f).toInt() % 2 == 0)
        if (!blink) {
            // Oyuncu araci garajda secilen sekil + boya ile cizilir. Trafik
            // ayni cizici ile ama SABIT bir sekil/palet ile cizilir
            // (drawObstacleCar) — tehdidin gorunumu her kosuda ayni olmali.
            drawStyledCar(
                x = engine.playerX,
                y = engine.playerY,
                style = engine.carStyle,
                boosting = engine.boosting,
                // Alevin titremesi kosunun zamanindan beslenir; ayri bir
                // animasyon durumu tutmuyoruz (motor zaten her kare adiyor).
                flamePhase = engine.timeElapsed,
                sprites = sprites
            )
        }
        drawParticles(engine)
    }
}

// ---------------------------------------------------------------------------
// Yol kenari temalari
// ---------------------------------------------------------------------------

private fun DrawScope.drawSideBackgrounds(engine: GameEngine) {
    val w = engine.viewWidth
    val h = engine.viewHeight
    val leftW = engine.roadX
    val rightX = engine.roadX + engine.roadWidth
    val rightW = w - rightX
    if (leftW <= 0f || h <= 0f) return

    when (engine.theme) {
        RoadTheme.GRASS -> {
            drawRect(Color(0xFF92D050), Offset(0f, 0f), Size(leftW, h))
            drawRect(Color(0xFF92D050), Offset(rightX, 0f), Size(rightW, h))
            val stroke = Color(0x0FFFFFFF)
            var y = -(engine.roadOffset * 0.22f) % 120f
            while (y < h + 120f) {
                drawLine(stroke, Offset(0f, y), Offset(leftW, y + 18f), strokeWidth = 2f)
                drawLine(stroke, Offset(rightX, y + 18f), Offset(w, y), strokeWidth = 2f)
                y += 120f
            }
        }

        RoadTheme.BEACH -> {
            drawRect(Color(0xFFEED9A0), Offset(0f, 0f), Size(leftW, h))
            drawRect(Color(0xFFEED9A0), Offset(rightX, 0f), Size(rightW, h))
            val waterW = max(22f, (leftW * 0.2f))
            drawRect(Color(0xFF5AC8FA), Offset(0f, 0f), Size(waterW, h))
            drawRect(Color(0xFF5AC8FA), Offset(w - waterW, 0f), Size(waterW, h))
            val foam = Color(0x61FFFFFF)
            var y = -(engine.roadOffset * 0.3f) % 80f
            while (y < h + 80f) {
                drawLine(foam, Offset(waterW - 3f, y), Offset(waterW + 2f, y + 18f), strokeWidth = 2f)
                drawLine(
                    foam,
                    Offset(w - waterW + 3f, y + 10f),
                    Offset(w - waterW - 2f, y + 28f),
                    strokeWidth = 2f
                )
                y += 80f
            }
        }

        RoadTheme.CROWD -> {
            drawRect(Color(0xFF6EA04C), Offset(0f, 0f), Size(leftW, h))
            drawRect(Color(0xFF6EA04C), Offset(rightX, 0f), Size(rightW, h))
            // Pist bariyerleri
            drawRect(Color(0xFFCFD8DC), Offset(leftW - 10f, 0f), Size(10f, h))
            drawRect(Color(0xFFCFD8DC), Offset(rightX, 0f), Size(10f, h))
            // Piksel seyirci bloklari
            val scroll = (engine.roadOffset * 0.35f) % 42f
            var y = -42f + scroll
            while (y < h + 42f) {
                var x = 3f
                while (x < leftW - 14f) {
                    drawRect(crowdColor(x, y), Offset(x, y), Size(3f, 5f))
                    x += 6f
                }
                x = rightX + 14f
                while (x < w - 3f) {
                    drawRect(crowdColor(x, y), Offset(x, y + 9f), Size(3f, 5f))
                    x += 6f
                }
                y += 42f
            }
            var flagY = -(engine.roadOffset * 0.18f) % 170f
            while (flagY < h + 170f) {
                drawMiniFlag(leftW * 0.18f, flagY + 28f)
                drawMiniFlag(rightX + rightW * 0.82f, flagY + 76f)
                flagY += 170f
            }
        }

        RoadTheme.NIGHT -> {
            val gradient = Brush.verticalGradient(
                listOf(Color(0xFF07111F), Color(0xFF0F2138)),
                startY = 0f,
                endY = h
            )
            drawRect(gradient, Offset(0f, 0f), Size(leftW, h))
            drawRect(gradient, Offset(rightX, 0f), Size(rightW, h))
            val line = Color(0x2E56E9FF)
            var y = -(engine.roadOffset * 0.28f) % 105f
            while (y < h + 105f) {
                drawLine(line, Offset(0f, y), Offset(leftW, y + 12f), strokeWidth = 2f)
                drawLine(line, Offset(rightX, y + 12f), Offset(w, y), strokeWidth = 2f)
                y += 105f
            }
            // Uzaktaki sehir isiklari
            var lightY = -(engine.roadOffset * 0.4f) % 88f
            while (lightY < h + 88f) {
                var x = 10f
                while (x < leftW - 8f) {
                    drawRect(cityLightColor(x), Offset(x, lightY), Size(3f, 7f))
                    x += 22f
                }
                x = rightX + 8f
                while (x < w - 10f) {
                    drawRect(cityLightColor(x), Offset(x, lightY + 12f), Size(3f, 7f))
                    x += 22f
                }
                lightY += 88f
            }
        }
    }
}

private fun crowdColor(x: Float, y: Float): Color = when (((x + y / 6f).toInt()) % 4) {
    0 -> Color(0xFFFFFFFF)
    1 -> Color(0xFFD62828)
    2 -> Color(0xFF1D3557)
    else -> Color(0xFF111111)
}

private fun cityLightColor(x: Float): Color =
    if (x.toInt() % 44 == 0) Color(0xFFFFD54F) else Color(0xFF90CAF9)

private fun DrawScope.drawMiniFlag(x: Float, y: Float) {
    drawLine(Color(0xFF444444), Offset(x, y), Offset(x, y + 20f), strokeWidth = 2f)
    val path = Path().apply {
        moveTo(x, y)
        lineTo(x + 10f, y + 4f)
        lineTo(x, y + 8f)
        close()
    }
    drawPath(path, Color(0xFFFFEB3B))
}

// ---------------------------------------------------------------------------
// Yol
// ---------------------------------------------------------------------------

private fun DrawScope.drawTrack(engine: GameEngine) {
    val h = engine.viewHeight
    if (h <= 0f) return
    drawRect(Color(0xFF3A4048), Offset(engine.roadX, 0f), Size(engine.roadWidth, h))

    // Kirmizi/beyaz kerb bloklari. Blok boyu ve renk kontrasti bilerek
    // dusuruldu — bkz. GameConfig.KERB_BLOCK_HEIGHT_PX (goz yorgunlugu).
    val block = GameConfig.KERB_BLOCK_HEIGHT_PX
    var y = -block
    while (y < h + block) {
        val red = ((((y + engine.roadOffset / 12f) / block).toInt()) % 2) == 0
        val color = if (red) Color(0xFFC8393B) else Color(0xFFDCE2E9)
        drawRect(color, Offset(engine.roadX - 8f, y), Size(8f, block))
        drawRect(color, Offset(engine.roadX + engine.roadWidth, y), Size(8f, block))
        y += block
    }

    // Kesik serit cizgileri: daha uzun ve daha seyrek, biraz da soluk.
    val on = GameConfig.LANE_DASH_ON_PX
    val off = GameConfig.LANE_DASH_OFF_PX
    val dash = PathEffect.dashPathEffect(floatArrayOf(on, off), 0f)
    val period = on + off
    val lineStart = -period + (engine.roadOffset % period)
    for (i in 1 until GameConfig.LANE_COUNT) {
        val x = engine.roadX + engine.laneWidth * i
        drawLine(
            color = Color(0xC2FFFFFF),
            start = Offset(x, lineStart),
            end = Offset(x, h + period),
            strokeWidth = 5f,
            pathEffect = dash
        )
    }

    // Asfalt dokusu (cok soluk noktalar)
    val speck = Color(0x08FFFFFF)
    for (i in 0 until 24) {
        val sx = engine.roadX + (i * 37f) % engine.roadWidth
        val sy = (i * 83f + engine.roadOffset * 1.4f) % h
        drawRect(speck, Offset(sx, sy), Size(3f, 3f))
    }
}

// ---------------------------------------------------------------------------
// Hiz gostergesi (sol serit disinda, prototipteki drawSpeedometer)
// ---------------------------------------------------------------------------

private fun DrawScope.drawSpeedometer(
    engine: GameEngine,
    density: Float,
    textMeasurer: TextMeasurer,
    valueSize: TextUnit,
    labelSize: TextUnit,
    smallSize: TextUnit
) {
    if (engine.viewHeight <= 0f) return
    val kmh = engine.speedKmh()
    val pct = kmh / GameConfig.SPEEDOMETER_MAX_KMH

    // Gosterge YOLUN USTUNE TASMAZ ve ekranin ortasinda durmaz.
    // Oyuncu geri bildirimi (2026-08-15): "hiz gostergesi cok buyuk bence ve
    // cok ortada". Eskiden `max(150, roadX)` ile taban genislik zorlaniyordu;
    // dar ekranlarda bu deger yolun ic tarafina tasiyor ve gosterge tam
    // oynanis alaninin uzerine oturuyordu.
    //
    // Artik yalnizca GERCEK sol bosluk (roadX) kullaniliyor: yaricap bosluga
    // gore kirpiliyor, merkez de dairenin sag kenari yola degmeyecek sekilde
    // sinirlaniyor. Dikeyde de ortadan yukari alindi (%32) — orta seride
    // bakan goz artik gostergeye takilmiyor.
    val marginPx = engine.roadX * density
    val r = min(min(marginPx * 0.40f, engine.viewHeight * density * 0.055f), 34f * density)
    if (r <= 8f * density) return
    val cx = min(marginPx * 0.5f, marginPx - r - 4f * density)
    val cy = engine.viewHeight * 0.32f * density

    val startDeg = -0.78f * 180f
    val sweepDeg = 0.78f * 2f * 180f
    val valueSweep = sweepDeg * pct

    // Gostergenin yumusak zemini (halkanin biraz disina tasar).
    drawCircle(
        brush = Brush.linearGradient(
            listOf(Color(0x2E061226), Color(0x8C000C1C)),
            start = Offset(cx - r, cy - r),
            end = Offset(cx + r, cy + r)
        ),
        radius = r + 7f * density,
        center = Offset(cx, cy)
    )

    val arcTopLeft = Offset(cx - r, cy - r)
    val arcSize = Size(r * 2, r * 2)

    // Cizgi kalinliklari da yaricapla olceklenir; sabit kalirlarsa kucuk
    // gostergede halka tamamen kapanip disk gibi gorunuyordu.
    val trackStroke = r * 0.20f
    val valueStroke = r * 0.16f

    drawArc(
        color = Color(0x14FFFFFF),
        startAngle = startDeg,
        sweepAngle = sweepDeg,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = trackStroke)
    )
    drawArc(
        color = Color(0xFF56E9FF),
        startAngle = startDeg,
        sweepAngle = valueSweep,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = valueStroke, cap = StrokeCap.Round)
    )

    fun text(value: String, color: Color, fontSize: TextUnit, italic: Boolean, center: Offset) {
        val layout = textMeasurer.measure(
            text = value,
            style = TextStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                fontFamily = FontFamily.SansSerif
            )
        )
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f)
        )
    }

    // Yalnizca hiz ve birimi. Prototipteki "375nm" (tork) ve "%100" (boost)
    // yazilari KALDIRILDI: tork tamamen susleme bir sayiydi, boost zaten
    // ekranin en ustundeki serit. Gosterge kuculunce ikisi de okunmuyordu.
    text("$kmh", Color(0xFFF8F0E2), valueSize, italic = true, center = Offset(cx, cy - r * 0.10f))
    text(
        "KM/H",
        Color(0xFF56E9FF),
        labelSize,
        italic = false,
        center = Offset(cx, cy + r * 0.62f)
    )
}

// ---------------------------------------------------------------------------
// Araclar, coinler, parcaciklar
// ---------------------------------------------------------------------------

/**
 * Trafikteki engel araci.
 *
 * 2026-08-15'te oyuncu araciyla AYNI cizim boru hattina bagladi
 * ([CarCatalog.trafficShape] + [drawCarBody]); onceden burada elle yazilmis
 * bir kopya vardi ve oyuncu araci yenilenince trafik geride kalirdi.
 *
 * Tehdit yine ayirt edilebilir kalir, iki ayri kanaldan:
 *  * palet — govde [GameEngine.OBSTACLE_COLORS]'tan gelir, surucu basi mavi
 *    (oyuncuda sari) ve oyuncu boyalari bu tonlarin DISINDA secilmistir;
 *  * siluet — trafik gövdesi bilerek daha kutu (kucuk kose yaricapi, kunt
 *    burun, serit yok denecek kadar soluk).
 *
 * Govde boyutu ve carpisma kutusu DEGISMEDI.
 */
private fun DrawScope.drawObstacleCar(
    x: Float,
    y: Float,
    bodyArgb: Long,
    sprites: CarSpriteSet?
) {
    translate(x, y) {
        // Cizim koordinatlari prototipin ham arac uzayina ait; tek bir olcek
        // carpaniyla kucultuluyor ki carpisma kutusu (GameConfig'te ayni
        // carpandan turetiliyor) gorselle birebir ortussun.
        scale(GameConfig.CAR_ART_SCALE, GameConfig.CAR_ART_SCALE, pivot = Offset.Zero) {
            drawCarShadow()
            drawCarBody(CarCatalog.trafficStyle(bodyArgb), sprites)
        }
    }
}

private fun DrawScope.drawCoin(coin: GameEngine.Coin) {
    val pulse = 1f + sin(coin.pulse) * 0.1f
    val radius = 10f * pulse
    drawCircle(Color(0xFFF6C000), radius, Offset(coin.x, coin.y))
    drawCircle(Color(0xFF8F6A00), radius, Offset(coin.x, coin.y), style = Stroke(width = 3f))
    drawRect(
        Color(0xFFFFF0A8),
        Offset(coin.x - 2f * pulse, coin.y - 6f * pulse),
        Size(4f * pulse, 12f * pulse)
    )
}

private fun DrawScope.drawNightHeadlights(engine: GameEngine) {
    if (engine.theme != RoadTheme.NIGHT) return
    val px = engine.playerX
    val py = engine.playerY
    val path = Path().apply {
        moveTo(px - 88f, py - 18f)
        quadraticTo(px, py - 230f, px + 88f, py - 18f)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(Color(0x38FFFAD2), Color(0x1FFFF8BE), Color(0x00FFF8BE)),
            center = Offset(px, py - 92f),
            radius = 170f
        )
    )
}

private fun DrawScope.drawParticles(engine: GameEngine) {
    engine.particles.forEach { p ->
        val alpha = (p.life * 1.8f).coerceIn(0f, 1f)
        drawRect(
            color = Color(p.color).copy(alpha = alpha),
            topLeft = Offset(p.x, p.y),
            size = Size(p.size, p.size)
        )
    }
}
