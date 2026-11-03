package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = OnElectricCyanContainer,
    secondary = SignalAmber,
    onSecondary = OnSignalAmber,
    secondaryContainer = SignalAmberContainer,
    onSecondaryContainer = OnSignalAmberContainer,
    tertiary = HyperIndigo,
    onTertiary = OnHyperIndigo,
    tertiaryContainer = HyperIndigoContainer,
    onTertiaryContainer = OnHyperIndigoContainer,
    error = CoralError,
    errorContainer = CoralErrorContainer,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = SlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = SlateSurfaceBright,
    outline = OutlineDark
)

private val StudioLightColorScheme = lightColorScheme(
    primary = DeepTealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F4D8),
    onPrimaryContainer = Color(0xFF002114),
    secondary = DeepAmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB2),
    onSecondaryContainer = Color(0xFF2A1800),
    tertiary = DeepIndigoTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDE1FF),
    onTertiaryContainer = Color(0xFF00105C),
    background = LightBackground,
    onBackground = Color(0xFF12161F),
    surface = LightSurface,
    onSurface = Color(0xFF12161F),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF444C5C),
    outline = Color(0xFFCBD2E0)
)

val StreamForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun StreamForgeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StreamForgeShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    StreamForgeTheme(darkTheme = darkTheme, content = content)
}
