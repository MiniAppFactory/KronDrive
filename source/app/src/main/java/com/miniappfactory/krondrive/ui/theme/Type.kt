package com.miniappfactory.krondrive.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Prototip "Arial Black" kullaniyordu; Android'de karsiligi sistem sans-serif
 * ailesinin en agir kesimidir (ek font dosyasi = ek APK boyutu, gerek yok).
 */
private val Heavy = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Black,
    letterSpacing = 0.6.sp
)

val KronTypography = Typography(
    displayLarge = Heavy.copy(fontSize = 42.sp, lineHeight = 46.sp),
    displayMedium = Heavy.copy(fontSize = 34.sp, lineHeight = 38.sp),
    headlineMedium = Heavy.copy(fontSize = 26.sp, lineHeight = 30.sp),
    titleLarge = Heavy.copy(fontSize = 20.sp, lineHeight = 24.sp),
    titleMedium = Heavy.copy(fontSize = 16.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.1.sp
    )
)
