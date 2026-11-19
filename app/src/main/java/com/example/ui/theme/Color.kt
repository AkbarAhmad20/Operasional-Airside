package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// --- Airside Night Ops (Dark Mode) Palette ---
// Derived from the Airside Operation InJourney Airports winged emblem (#152B32 slate + #F4B918 gold)
val NightSlateDeep = Color(0xFF0C1821)
val NightSlateSurface = Color(0xFF152B32)
val NightSlateContainer = Color(0xFF1C353E)
val NightSlateContainerHigh = Color(0xFF23414C)
val AirsideGoldBright = Color(0xFFF5BC1D)
val AirsideGoldContainer = Color(0xFF3E310B)
val AirsideGoldOnContainer = Color(0xFFFFE082)
val NightIceWhite = Color(0xFFF1F6F9)
val NightMutedSlate = Color(0xFFA8BEC8)
val NightOutline = Color(0xFF33505C)
val NightOutlineVariant = Color(0xFF233C46)
val NightTealAccent = Color(0xFF38BDF8)

// --- Daylight Tarmac High-Contrast (Light Mode) Palette ---
// Engineered for direct sunlight readability on airport aprons
val DaylightCanvas = Color(0xFFF2F6F9)
val DaylightSurface = Color(0xFFFFFFFF)
val DaylightContainer = Color(0xFFE6EEF3)
val DaylightContainerHigh = Color(0xFFD8E4EC)
val InJourneyNavy = Color(0xFF102833)
val InJourneyNavyContainer = Color(0xFFD1E5EE)
val DaylightGoldAccent = Color(0xFFB37B00)
val DaylightGoldContainer = Color(0xFFFFF0C2)
val DaylightGoldOnContainer = Color(0xFF3D2800)
val DaylightInk = Color(0xFF0D1D25)
val DaylightMutedInk = Color(0xFF425B68)
val DaylightOutline = Color(0xFF9EB3BF)
val DaylightOutlineVariant = Color(0xFFCFDCE4)

// --- Operational Semantic Status Colors ---
val DockedEmeraldDark = Color(0xFF34D399)
val DockedEmeraldContainerDark = Color(0xFF0D3326)
val DockedEmeraldLight = Color(0xFF047857)
val DockedEmeraldContainerLight = Color(0xFFD1FAE5)

val UndockingAmberDark = Color(0xFFFBBF24)
val UndockingAmberContainerDark = Color(0xFF3B2B0A)
val UndockingAmberLight = Color(0xFFB45309)
val UndockingAmberContainerLight = Color(0xFFFEF3C7)

val VerifiedSkyDark = Color(0xFF38BDF8)
val VerifiedSkyContainerDark = Color(0xFF0C2E42)
val VerifiedSkyLight = Color(0xFF0369A1)
val VerifiedSkyContainerLight = Color(0xFFE0F2FE)

val DangerRedDark = Color(0xFFF87171)
val DangerRedContainerDark = Color(0xFF451212)
val DangerRedLight = Color(0xFFB91C1C)
val DangerRedContainerLight = Color(0xFFFEE2E2)

@Immutable
data class OperationalColors(
    val docked: Color,
    val dockedContainer: Color,
    val undocking: Color,
    val undockingContainer: Color,
    val verified: Color,
    val verifiedContainer: Color,
    val pending: Color,
    val pendingContainer: Color,
    val goldAccent: Color,
    val goldContainer: Color,
    val onGoldContainer: Color,
    val heroBannerStart: Color,
    val heroBannerEnd: Color,
    val cardBorder: Color,
)

val DarkOperationalColors = OperationalColors(
    docked = DockedEmeraldDark,
    dockedContainer = DockedEmeraldContainerDark,
    undocking = UndockingAmberDark,
    undockingContainer = UndockingAmberContainerDark,
    verified = VerifiedSkyDark,
    verifiedContainer = VerifiedSkyContainerDark,
    pending = UndockingAmberDark,
    pendingContainer = UndockingAmberContainerDark,
    goldAccent = AirsideGoldBright,
    goldContainer = AirsideGoldContainer,
    onGoldContainer = AirsideGoldOnContainer,
    heroBannerStart = Color(0xFF10242B),
    heroBannerEnd = Color(0xFF1A3842),
    cardBorder = Color(0xFF284854),
)

val LightOperationalColors = OperationalColors(
    docked = DockedEmeraldLight,
    dockedContainer = DockedEmeraldContainerLight,
    undocking = UndockingAmberLight,
    undockingContainer = UndockingAmberContainerLight,
    verified = VerifiedSkyLight,
    verifiedContainer = VerifiedSkyContainerLight,
    pending = UndockingAmberLight,
    pendingContainer = UndockingAmberContainerLight,
    goldAccent = DaylightGoldAccent,
    goldContainer = DaylightGoldContainer,
    onGoldContainer = DaylightGoldOnContainer,
    heroBannerStart = Color(0xFF102833),
    heroBannerEnd = Color(0xFF1B3F4E),
    cardBorder = Color(0xFFC8D7E0),
)

val LocalOperationalColors = staticCompositionLocalOf { DarkOperationalColors }
