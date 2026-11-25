package com.sagay.touristnav.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Deep Ocean & Sunrise" brand tokens.
 *
 * The chrome uses a calm deep-ocean palette so the map and the functional
 * terminal colours remain the heroes. The warm sunrise accent is reserved for
 * what matters: the FAB, the fare, and the primary actions.
 *
 * NOTE: the route colours themselves (red / pink / yellow / white) come from
 * seed data via Route.colorHex and are deliberately NOT defined here.
 */
object SagayColors {
    // New palette.
    val DeepOcean = Color(0xFF0B3C5D)
    val DeepOceanDark = Color(0xFF072A42)
    val Coral = Color(0xFFFF6B52)
    val SunriseGold = Color(0xFFFFB347)
    val WarmMist = Color(0xFFF7F9F8)
    val Ink = Color(0xFF12212B)
    val SecondaryTeal = Color(0xFF1B7A78)
    val InkMuted = Color(0xFF5A6B75)
    val SoftHairline = Color(0xFFDCE6E7)
    val CardWhite = Color(0xFFFFFFFF)

    // Legacy aliases kept so every existing call site keeps compiling and
    // automatically picks up the new identity.
    val TealPrimary = DeepOcean
    val TealDark = DeepOceanDark
    val SunriseDeep = Coral
    val SunriseMid = Color(0xFFF05A3C)
    val SunriseLight = SunriseGold
    val SunriseGlow = Color(0xFFFFD08A)
    val MistSurface = WarmMist
    val TextDark = Ink
    val TextMuted = InkMuted
    val Hairline = SoftHairline

    // Functional colours: unchanged. These are not route colours from the
    // seed, but they carry meaning in the UI (user pin, road line, boat note,
    // errors) and must keep reading the same way.
    val RouteBlue = Color(0xFF0277BD)
    val UserBlue = Color(0xFF0277BD)
    val WarningRed = Color(0xFFC62828)
    val BoatTeal = Color(0xFF00695C)
    val UnknownGrey = Color(0xFF9E9E9E)

    /** The sunrise motif, retinted coral -> gold. */
    val sunriseGradient = Brush.horizontalGradient(
        colors = listOf(Coral, SunriseGold),
    )

    /** The smaller banner strip uses the same motif. */
    val bannerGradient = Brush.horizontalGradient(
        colors = listOf(Coral, SunriseGold),
    )
}

private val LightColors = lightColorScheme(
    primary = SagayColors.DeepOcean,
    onPrimary = Color.White,
    primaryContainer = SagayColors.Coral,
    onPrimaryContainer = Color.White,
    secondary = SagayColors.SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = SagayColors.SecondaryTeal.copy(alpha = 0.14f),
    onSecondaryContainer = SagayColors.DeepOceanDark,
    tertiary = SagayColors.DeepOceanDark,
    onTertiary = Color.White,
    tertiaryContainer = SagayColors.SunriseGold.copy(alpha = 0.24f),
    onTertiaryContainer = SagayColors.Ink,
    background = SagayColors.WarmMist,
    onBackground = SagayColors.Ink,
    surface = SagayColors.WarmMist,
    onSurface = SagayColors.Ink,
    surfaceVariant = SagayColors.CardWhite,
    onSurfaceVariant = SagayColors.InkMuted,
    surfaceTint = Color.Transparent,
    outline = SagayColors.SoftHairline,
    error = SagayColors.WarningRed,
    onError = Color.White,
)

/**
 * Modern type scale: heavier display/headline weights, tighter tracking on
 * large text, and clearer line height on body copy. Font sizes and all other
 * slots are inherited from Material 3 so the scale stays internally coherent.
 */
private val baseTypography = Typography()
private val SagayTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = baseTypography.displayMedium.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1.2).sp,
    ),
    displaySmall = baseTypography.displaySmall.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.9).sp,
    ),
    headlineLarge = baseTypography.headlineLarge.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.7).sp,
    ),
    headlineMedium = baseTypography.headlineMedium.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = baseTypography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = baseTypography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = baseTypography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = baseTypography.titleSmall.copy(
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = baseTypography.bodyLarge.copy(
        lineHeight = 25.sp,
    ),
    bodyMedium = baseTypography.bodyMedium.copy(
        letterSpacing = 0.1.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = baseTypography.bodySmall.copy(
        letterSpacing = 0.1.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = baseTypography.labelLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
    ),
    labelMedium = baseTypography.labelMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = baseTypography.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
    ),
)

/** Rounded, friendly shapes with soft elevation everywhere new. */
private val SagayShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SagayTheme(content: @Composable () -> Unit) {
    // The paper specifies a light theme; dark is intentionally not themed.
    isSystemInDarkTheme() // observed but ignored
    MaterialTheme(
        colorScheme = LightColors,
        typography = SagayTypography,
        shapes = SagayShapes,
        content = content,
    )
}
