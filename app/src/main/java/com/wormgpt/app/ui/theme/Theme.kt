package com.wormgpt.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WormGptDarkColorScheme = darkColorScheme(
    primary = WormRed,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = WormRedDark,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
    secondary = WormRedLight,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = Black,
    onBackground = androidx.compose.ui.graphics.Color.White,
    surface = SurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = WormRed,
    onError = androidx.compose.ui.graphics.Color.White
)

@Composable
fun WormGptTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = WormGptDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Black.toArgb()
            window.navigationBarColor = Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WormGptTypography,
        content = content
    )
}
