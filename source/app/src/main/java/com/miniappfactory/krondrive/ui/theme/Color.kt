package com.miniappfactory.krondrive.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Renkler HTML prototipinden BIREBIR alindi — native surumun ilk bakista ayni
 * oyun gibi gorunmesi icin. Yeni bir renk gerekiyorsa once burada tanimla.
 */
object KronColors {
    /** Sayfa zemini (body radial-gradient'in en koyu durak rengi). */
    val Background = Color(0xFF020915)
    val BackgroundMid = Color(0xFF051427)
    val BackgroundTop = Color(0xFF0C2A52)

    /** Kart/panel zeminleri (menu-card, result-card). */
    val Surface = Color(0xFF0A1A33)
    val SurfaceDeep = Color(0xFF040D1B)
    val SurfaceBorder = Color(0x14FFFFFF)

    /** Marka sarisi (.title, .btn.primary). */
    val Accent = Color(0xFFF5C100)
    val AccentBright = Color(0xFFFFD43D)

    /** Vurgu mavisi (.mode.active, boost). */
    val Blue = Color(0xFF169DFF)
    val BlueBright = Color(0xFF56E9FF)

    val TextPrimary = Color(0xFFEAF1FB)
    val TextSecondary = Color(0xFFB9C6D7)
    val TextMuted = Color(0xFF8092AB)

    /** Oyuncu aracinin rengi ve tehlike/fren vurgusu. */
    val PlayerRed = Color(0xFFE10600)
    val Danger = Color(0xFFFF5050)

    /** Yol ve serit cizgileri. */
    val Road = Color(0xFF3A4048)
    val RoadLine = Color(0xFFEFEFEF)
    val KerbRed = Color(0xFFD62828)

    /** Coin. */
    val Coin = Color(0xFFF6C000)

    /** Kilitli bolum/kart. */
    val Locked = Color(0xFF1C2740)
}
