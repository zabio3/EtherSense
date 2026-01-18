package com.ethersense.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = BackgroundDark,
    primaryContainer = CyanDark,
    onPrimaryContainer = TextPrimary,
    secondary = PurpleSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = PurpleDark,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentGreen,
    onTertiary = BackgroundDark,
    tertiaryContainer = SignalGood,
    onTertiaryContainer = BackgroundDark,
    error = AccentRed,
    onError = TextPrimary,
    errorContainer = SignalWeak,
    onErrorContainer = TextPrimary,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary,
    outlineVariant = TextDisabled,
    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundDark,
    inversePrimary = CyanDark
)

@Composable
fun EtherSenseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EtherSenseTypography,
        content = content
    )
}
