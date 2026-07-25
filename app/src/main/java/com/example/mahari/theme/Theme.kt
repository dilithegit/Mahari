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
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BlueContainerLight,
    onPrimaryContainer = BlueDeepNavy,
    
    secondary = GreenPrimary,
    onSecondary = Color.White,
    secondaryContainer = GreenContainerLight,
    onSecondaryContainer = GreenContainerDark,
    
    background = NeutralBgLight,
    onBackground = NeutralTextPrimaryLight,
    
    surface = NeutralSurfaceLight,
    onSurface = NeutralTextPrimaryLight,
    surfaceVariant = BlueContainerLight,
    onSurfaceVariant = NeutralTextSecondaryLight,
    
    outline = NeutralBorderLight,
    
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedContainer,
    onErrorContainer = AlertRed
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueLight,
    onPrimary = BlueDeepNavy,
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = BlueSoft,
    
    secondary = GreenLight,
    onSecondary = GreenContainerDark,
    secondaryContainer = GreenContainerDark,
    onSecondaryContainer = GreenLight,
    
    background = NeutralBgDark,
    onBackground = NeutralTextPrimaryDark,
    
    surface = NeutralSurfaceDark,
    onSurface = NeutralTextPrimaryDark,
    surfaceVariant = NeutralBorderDark,
    onSurfaceVariant = NeutralTextSecondaryDark,
    
    outline = NeutralBorderDark,
    
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
