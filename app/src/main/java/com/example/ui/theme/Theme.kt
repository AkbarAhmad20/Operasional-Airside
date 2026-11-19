package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val AirsideDarkColorScheme = darkColorScheme(
    primary = AirsideGoldBright,
    onPrimary = Color(0xFF1A1400),
    primaryContainer = AirsideGoldContainer,
    onPrimaryContainer = AirsideGoldOnContainer,
    secondary = NightTealAccent,
    onSecondary = Color(0xFF002130),
    secondaryContainer = Color(0xFF10384A),
    onSecondaryContainer = Color(0xFFC8EEFF),
    tertiary = DockedEmeraldDark,
    onTertiary = Color(0xFF00291B),
    tertiaryContainer = DockedEmeraldContainerDark,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = NightSlateDeep,
    onBackground = NightIceWhite,
    surface = NightSlateSurface,
    onSurface = NightIceWhite,
    surfaceVariant = NightSlateContainer,
    onSurfaceVariant = NightMutedSlate,
    surfaceContainerLowest = Color(0xFF09131A),
    surfaceContainerLow = Color(0xFF102029),
    surfaceContainer = NightSlateSurface,
    surfaceContainerHigh = NightSlateContainer,
    surfaceContainerHighest = NightSlateContainerHigh,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant,
    error = DangerRedDark,
    onError = Color(0xFF3F0606),
    errorContainer = DangerRedContainerDark,
    onErrorContainer = Color(0xFFFECACA),
)

private val DaylightLightColorScheme = lightColorScheme(
    primary = InJourneyNavy,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = InJourneyNavyContainer,
    onPrimaryContainer = Color(0xFF091B24),
    secondary = DaylightGoldAccent,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = DaylightGoldContainer,
    onSecondaryContainer = DaylightGoldOnContainer,
    tertiary = DockedEmeraldLight,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = DockedEmeraldContainerLight,
    onTertiaryContainer = Color(0xFF023E2D),
    background = DaylightCanvas,
    onBackground = DaylightInk,
    surface = DaylightSurface,
    onSurface = DaylightInk,
    surfaceVariant = DaylightContainer,
    onSurfaceVariant = DaylightMutedInk,
    surfaceContainerLowest = DaylightSurface,
    surfaceContainerLow = Color(0xFFF7FAFC),
    surfaceContainer = DaylightSurface,
    surfaceContainerHigh = DaylightContainer,
    surfaceContainerHighest = DaylightContainerHigh,
    outline = DaylightOutline,
    outlineVariant = DaylightOutlineVariant,
    error = DangerRedLight,
    onError = Color(0xFFFFFFFF),
    errorContainer = DangerRedContainerLight,
    onErrorContainer = Color(0xFF5C0A0A),
)

val GarbarataShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun GarbarataOpsTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AirsideDarkColorScheme else DaylightLightColorScheme
    val operationalColors = if (darkTheme) DarkOperationalColors else LightOperationalColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalOperationalColors provides operationalColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GarbarataTypography,
            shapes = GarbarataShapes,
            content = content
        )
    }
}
