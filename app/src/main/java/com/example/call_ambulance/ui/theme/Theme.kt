package com.example.call_ambulance.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFCE2029),      // Main header blue
    onPrimary = Color.White,

    secondary = Color(0xFF164972),    // For cards, etc.
    onSecondary = Color.White,

    background = Color(0xFFF5F5F5),
    onBackground = Color.Black,

    surface = Color.White,
    onSurface = Color.Black,

    error = Color(0xFFC62828),
    onError = Color.White
)

@Composable
fun CallSupportTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val window = (view.context as Activity).window

    // ✅ Set global status bar color
    SideEffect {
        window.statusBarColor = LightColorScheme.primary.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
