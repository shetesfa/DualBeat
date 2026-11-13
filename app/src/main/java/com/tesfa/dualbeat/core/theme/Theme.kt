package com.tesfa.dualbeat.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

enum class AccentChoice(val hexColor: Long, val displayName: String) {
    NEON_CYAN(0xFF00E5FF, "Electric Cyan"),
    DEEP_VIOLET(0xFF7C4DFF, "Deep Violet"),
    HOT_PINK(0xFFFF0055, "Neon Pink"),
    EMERALD(0xFF00E676, "Emerald Beat"),
    AMBER(0xFFFFAB00, "Amber Glow")
}

@Composable
fun DualBeatTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED,
    accentChoice: AccentChoice = AccentChoice.NEON_CYAN,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val isAmoled = themeMode == ThemeMode.AMOLED

    val accent = Color(accentChoice.hexColor)

    val colorScheme = if (isDark) {
        val bg = if (isAmoled) AmoledBackground else DarkSurface
        val card = if (isAmoled) DarkSurface else DarkSurfaceCard

        darkColorScheme(
            primary = accent,
            onPrimary = AmoledBackground,
            primaryContainer = accent.copy(alpha = 0.2f),
            onPrimaryContainer = accent,
            secondary = DeepViolet,
            onSecondary = TextWhitePrimary,
            background = bg,
            onBackground = TextWhitePrimary,
            surface = bg,
            onSurface = TextWhitePrimary,
            surfaceVariant = card,
            onSurfaceVariant = TextMutedSecondary,
            outline = DarkBorder,
            error = ErrorRed,
            onError = TextWhitePrimary
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = LightSurface,
            primaryContainer = accent.copy(alpha = 0.15f),
            onPrimaryContainer = DarkSurface,
            secondary = DeepViolet,
            onSecondary = LightSurface,
            background = LightBackground,
            onBackground = TextDarkPrimary,
            surface = LightSurface,
            onSurface = TextDarkPrimary,
            surfaceVariant = LightSurfaceCard,
            onSurfaceVariant = TextDarkSecondary,
            outline = LightBorder,
            error = ErrorRed,
            onError = LightSurface
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
