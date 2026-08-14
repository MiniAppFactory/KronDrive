package com.miniappfactory.krondrive.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.miniappfactory.krondrive.game.CarCatalog
import com.miniappfactory.krondrive.game.CarPart
import com.miniappfactory.krondrive.game.CarStyle
import com.miniappfactory.krondrive.game.GameConfig

/**
 * Oyuncu aracinin cizimi — TEK kaynak. Hem oyun sahnesi
 * ([com.miniappfactory.krondrive.ui.game.drawGameScene]) hem de garajdaki
 * onizleme bu fonksiyonlari kullanir; boylece garajda gordugun arac ile
 * yolda surdugun arac ayni koddan cikar.
 *
 * Sekle ozel Compose kodu YOKTUR: geometri [CarCatalog] icinde saf veri olarak
 * durur, burasi sadece o veriyi Canvas'a cevirir. Yeni sekil eklemek icin bu
 * dosyaya dokunmak gerekmez.
 *
 * Koordinatlar prototipin ham arac uzayindadir (x -20..20, y -2..74); olcegi
 * cagiran uygular.
 */

/** Yere dusen golge — sekilden BAGIMSIZ, aracin ayak izi hissi sabit kalsin diye. */
fun DrawScope.drawCarShadow() {
    drawOval(
        color = Color(CarCatalog.SHADOW_ARGB),
        topLeft = Offset(CarCatalog.SHADOW_LEFT, CarCatalog.SHADOW_TOP),
        size = Size(CarCatalog.SHADOW_WIDTH, CarCatalog.SHADOW_HEIGHT)
    )
}

/** Katalogdaki parcalari sirayla cizer (liste sirasi = katman sirasi). */
fun DrawScope.drawCarParts(style: CarStyle) {
    style.shape.parts.forEach { part ->
        val color = Color(style.argbOf(part.paint))
        when (part) {
            is CarPart.Box -> if (part.corner > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(part.left, part.top),
                    size = Size(part.width, part.height),
                    cornerRadius = CornerRadius(part.corner)
                )
            } else {
                drawRect(
                    color = color,
                    topLeft = Offset(part.left, part.top),
                    size = Size(part.width, part.height)
                )
            }

            is CarPart.Disc -> drawCircle(
                color = color,
                radius = part.radius,
                center = Offset(part.centerX, part.centerY)
            )

            is CarPart.Wedge -> {
                val path = Path().apply {
                    part.points.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                    close()
                }
                drawPath(path, color)
            }
        }
    }
}

/**
 * Boost alevi. Aracin bittigi yerden baslar — kisa gövdeli sekillerde egzoz
 * ile alev arasinda bosluk kalmasin diye sabit bir y kullanilmiyor.
 */
fun DrawScope.drawCarBoostFlame(style: CarStyle) {
    val start = style.shape.artBottom + CarCatalog.FLAME_GAP
    val outer = Path().apply {
        moveTo(-CarCatalog.FLAME_OUTER_HALF_WIDTH, start)
        lineTo(CarCatalog.FLAME_OUTER_HALF_WIDTH, start)
        lineTo(0f, start + CarCatalog.FLAME_OUTER_LENGTH)
        close()
    }
    drawPath(outer, Color(CarCatalog.FLAME_OUTER_ARGB))
    val inner = Path().apply {
        moveTo(-CarCatalog.FLAME_INNER_HALF_WIDTH, start)
        lineTo(CarCatalog.FLAME_INNER_HALF_WIDTH, start)
        lineTo(0f, start + CarCatalog.FLAME_INNER_LENGTH)
        close()
    }
    drawPath(inner, Color(CarCatalog.FLAME_INNER_ARGB))
}

/**
 * Oyuncu araci: golge + gövde + (boost basiliysa) alev.
 * [GameConfig.CAR_ART_SCALE] burada uygulanir — carpisma kutusu ayni sabitten
 * turetildigi icin gorsel ile kutu birebir ortusur.
 */
fun DrawScope.drawStyledCar(x: Float, y: Float, style: CarStyle, boosting: Boolean) {
    translate(x, y) {
        scale(GameConfig.CAR_ART_SCALE, GameConfig.CAR_ART_SCALE, pivot = Offset.Zero) {
            drawCarShadow()
            drawCarParts(style)
            if (boosting) drawCarBoostFlame(style)
        }
    }
}

/**
 * Garaj onizlemesi. Arac, verilen alana golgesiyle birlikte ORTALANIR ve
 * sigacak kadar buyutulur; hangi sekil secilirse secilsin ayni cerceveyi
 * doldurur (kutu sabit — bkz. CarCatalog kutu kurali).
 */
@Composable
fun CarPreview(style: CarStyle, modifier: Modifier = Modifier, boosting: Boolean = false) {
    Canvas(modifier = modifier) {
        // Onizleme kutusu = katalogun garanti ettigi arac kutusu + golge alani.
        val left = minOf(CarCatalog.ART_LEFT, CarCatalog.SHADOW_LEFT)
        val right = maxOf(CarCatalog.ART_RIGHT, CarCatalog.SHADOW_LEFT + CarCatalog.SHADOW_WIDTH)
        val top = minOf(CarCatalog.ART_TOP, CarCatalog.SHADOW_TOP)
        val bottom = maxOf(CarCatalog.ART_BOTTOM, CarCatalog.SHADOW_TOP + CarCatalog.SHADOW_HEIGHT)
        val artWidth = right - left
        val artHeight = bottom - top
        if (artWidth <= 0f || artHeight <= 0f) return@Canvas

        val fit = minOf(size.width / artWidth, size.height / artHeight)
        translate(size.width / 2f, size.height / 2f) {
            scale(fit, fit, pivot = Offset.Zero) {
                translate(-(left + right) / 2f, -(top + bottom) / 2f) {
                    drawCarShadow()
                    drawCarParts(style)
                    if (boosting) drawCarBoostFlame(style)
                }
            }
        }
    }
}
