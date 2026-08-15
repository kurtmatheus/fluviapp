package dev.matheus.fluviapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Tema escuro: fundo mais profundo, texto claro, accent teal claro.
val DarkColors = darkColorScheme(
    primary = AquaAccent,
    onPrimary = AbyssNavy,
    secondary = HeaderNavy,
    onSecondary = MistGray,
    tertiary = SteelTeal,
    background = AbyssNavy,
    onBackground = MistGray,
    surface = HeaderNavy,
    onSurface = MistGray,
    outline = SteelTeal,
    primaryContainer = HeaderNavy,
    onPrimaryContainer = AquaAccent
)

// Tema claro: inverte fundo↔texto usando a mesma paleta; header navy fixo (marca).
val LightColors = lightColorScheme(
    primary = SteelTeal,
    onPrimary = MistGray,
    secondary = HeaderNavy,
    onSecondary = MistGray,
    tertiary = AquaAccent,
    background = MistGray,
    onBackground = AbyssNavy,
    surface = SurfaceLight,
    onSurface = AbyssNavy,
    outline = SteelTeal,
    primaryContainer = SurfaceLight,
    onPrimaryContainer = HeaderNavy
)

@Composable
fun FluviAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Header navy fixo nos dois temas → status bar acompanha, com ícones claros.
            window.statusBarColor = HeaderNavy.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}