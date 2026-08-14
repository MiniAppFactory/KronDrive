package com.miniappfactory.krondrive.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Oyun tek bir koyu temaya sahiptir (prototipte de oyle) — acik tema yok,
 * cunku yol/HUD renkleri koyu zemine gore tasarlandi.
 */
private val KronColorScheme = darkColorScheme(
    primary = KronColors.Accent,
    onPrimary = KronColors.Background,
    secondary = KronColors.Blue,
    onSecondary = KronColors.TextPrimary,
    background = KronColors.Background,
    onBackground = KronColors.TextPrimary,
    surface = KronColors.Surface,
    onSurface = KronColors.TextPrimary,
    error = KronColors.Danger,
    onError = KronColors.TextPrimary
)

@Composable
fun KronDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KronColorScheme,
        typography = KronTypography,
        content = content
    )
}
