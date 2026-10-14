package com.presetpilot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// PresetPilot stage-dark palette: near-black, amber accents (footswitch vibe).
private val Amber = Color(0xFFFFB300)
private val AmberDim = Color(0xFF8A6100)
private val StageBlack = Color(0xFF0B0D12)
private val PanelDark = Color(0xFF141926)
private val MutedGrey = Color(0xFF8A93A6)
private val GreenOk = Color(0xFF7CFC9A)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color.Black,
    secondary = GreenOk,
    background = StageBlack,
    onBackground = Color.White,
    surface = PanelDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C2333),
    onSurfaceVariant = MutedGrey,
    error = Color(0xFFFF6B6B),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE69500),
    secondary = Color(0xFF2E7D32),
)

@Composable
fun PresetPilotTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
