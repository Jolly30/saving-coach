package com.savingcoach.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MatchaPrimary,
    onPrimary = Color.White,
    primaryContainer = CreamSurfaceVariant,
    onPrimaryContainer = DarkRoast,
    secondary = WarmCaramel,
    onSecondary = Color.White,
    secondaryContainer = WarmCaramelContainer,
    onSecondaryContainer = WarmCaramelVariant,
    background = CreamBackground,
    onBackground = DarkRoast,
    surface = CreamSurface,
    onSurface = DarkRoast,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = EarthySlate,
    surfaceContainer = CreamSurface,
    surfaceContainerLow = CreamBackground,
    surfaceContainerHigh = CreamSurfaceVariant,
    surfaceContainerHighest = CreamSurfaceVariant,
    surfaceDim = CreamSurfaceVariant,
    surfaceBright = CreamSurface,
    outline = CreamOutline,
    outlineVariant = CreamOutlineVariant,
    error = CoralRed,
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = CoralRedDark
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkMatchaPrimary,
    onPrimary = Color(0xFF141412),
    primaryContainer = DarkMatchaPrimaryContainer,
    onPrimaryContainer = Color(0xFFD2EED8),
    secondary = DarkMatchaSecondary,
    onSecondary = Color(0xFF141412),
    secondaryContainer = Color(0xFF3D2A1C),
    onSecondaryContainer = Color(0xFFFBE4D2),
    background = DarkMatchaBackground,
    onBackground = DarkMatchaOnBackground,
    surface = DarkMatchaSurface,
    onSurface = DarkMatchaOnSurface,
    surfaceVariant = DarkMatchaSurfaceVariant,
    onSurfaceVariant = DarkMatchaOnSurfaceVariant,
    surfaceContainer = DarkMatchaSurface,
    surfaceContainerLow = DarkMatchaBackground,
    surfaceContainerHigh = DarkMatchaSurfaceVariant,
    surfaceContainerHighest = DarkMatchaSurfaceVariant,
    surfaceDim = DarkMatchaBackground,
    surfaceBright = DarkMatchaSurface,
    outline = DarkMatchaOutline,
    outlineVariant = DarkMatchaOutlineVariant,
    error = CoralRed,
    onError = Color.White,
    errorContainer = Color(0xFF5A1E1E),
    onErrorContainer = Color(0xFFFFB4AB)
)

@Composable
fun SavingCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
