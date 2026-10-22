package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = RqeebPrimaryDarkTheme,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0C4A6E),
    tertiary = RqeebAccentTeal,
    background = RqeebBackgroundDark,
    onBackground = RqeebTextPrimaryDark,
    surface = RqeebSurfaceDark,
    onSurface = RqeebTextPrimaryDark,
    surfaceVariant = RqeebSurfaceVariantDark,
    onSurfaceVariant = RqeebTextSecondaryDark,
    outline = RqeebBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = RqeebPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = RqeebSecondary,
    onSecondary = Color.White,
    tertiary = RqeebAccentTeal,
    background = RqeebBackgroundLight,
    onBackground = RqeebTextPrimaryLight,
    surface = RqeebSurfaceLight,
    onSurface = RqeebTextPrimaryLight,
    surfaceVariant = RqeebSurfaceVariantLight,
    onSurfaceVariant = RqeebTextSecondaryLight,
    outline = RqeebBorderLight
)

@Composable
fun RqeebTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isRtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
