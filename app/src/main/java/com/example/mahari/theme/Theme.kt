package com.example.mahari.theme

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

private val LightColorScheme = lightColorScheme(
    primary = EmeraldAccentLight,
    onPrimary = Color.White,
    primaryContainer = WarmBoneContainerLight,
    onPrimaryContainer = CharcoalTextPrimaryLight,

    secondary = EmeraldMid,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = EmeraldAccentLight,

    background = WarmBoneBgLight,
    onBackground = CharcoalTextPrimaryLight,

    surface = WarmBoneCardLight,
    onSurface = CharcoalTextPrimaryLight,
    surfaceVariant = WarmBoneContainerLight,
    onSurfaceVariant = CharcoalTextSecondaryLight,

    outline = Color(0xFFD6D4CC),

    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedContainer,
    onErrorContainer = AlertRed
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldAccentDark,
    onPrimary = CharcoalBgDark,
    primaryContainer = CharcoalElevatedDark,
    onPrimaryContainer = EmeraldAccentDark,

    secondary = EmeraldMid,
    onSecondary = CharcoalBgDark,
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = EmeraldAccentDark,

    background = CharcoalBgDark,
    onBackground = CharcoalTextPrimaryDark,

    surface = CharcoalCardDark,
    onSurface = CharcoalTextPrimaryDark,
    surfaceVariant = CharcoalElevatedDark,
    onSurfaceVariant = CharcoalTextSecondaryDark,

    outline = Color(0xFF33383D),

    error = AlertRed,
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFCA5A5)
)

@Composable
fun MahariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

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
