package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTealDark,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF9EEFFD),
    secondary = SecondaryTealDark,
    onSecondary = Color(0xFF1B3439),
    secondaryContainer = Color(0xFF324B4F),
    onSecondaryContainer = Color(0xFFCCE8ED),
    tertiary = TertiaryBlueDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EEFFD),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8ED),
    onSecondaryContainer = Color(0xFF051F23),
    tertiary = TertiaryBlue,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = Color(0xFFBA1A1A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use consistent brand theme across all Yemeni teacher devices
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
