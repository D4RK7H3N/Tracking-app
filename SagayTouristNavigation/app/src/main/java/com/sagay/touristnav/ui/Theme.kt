package com.sagay.touristnav.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * "Sunrise Route" brand tokens.
 *
 * The chrome stays calm teal/neutral so the map and the functional terminal
 * colors (red/green/blue) remain the heroes; the warm sunrise accent is
 * reserved for what matters: the FAB, the fare, and primary actions.
 */
object SagayColors {
    val TealPrimary = Color(0xFF00695C)
    val TealDark = Color(0xFF004D40)

    val SunriseDeep = Color(0xFFE65100)
    val SunriseMid = Color(0xFFF4511E)
    val SunriseLight = Color(0xFFFF8F00)
    val SunriseGlow = Color(0xFFFFB74D)

    val MistSurface = Color(0xFFF4FAF9)
    val CardWhite = Color(0xFFFFFFFF)
    val TextDark = Color(0xFF263238)
    val TextMuted = Color(0xFF78909C)
    val Hairline = Color(0xFFE0ECEA)

    val RouteBlue = Color(0xFF0277BD)
    val UserBlue = Color(0xFF0277BD)
    val WarningRed = Color(0xFFC62828)
    val BoatTeal = Color(0xFF00695C)

    val UnknownGrey = Color(0xFF9E9E9E)

    val sunriseGradient = Brush.verticalGradient(
        colors = listOf(SunriseLight, SunriseMid, Color(0xFFD84315)),
    )

    val bannerGradient = Brush.horizontalGradient(
        colors = listOf(SunriseLight, SunriseDeep),
    )
}

private val LightColors = lightColorScheme(
    primary = SagayColors.TealPrimary,
    onPrimary = Color.White,
    primaryContainer = SagayColors.SunriseLight,
    onPrimaryContainer = Color(0xFF5D2A00),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00332C),
    tertiary = SagayColors.TealDark,
    background = SagayColors.MistSurface,
    onBackground = SagayColors.TextDark,
    surface = SagayColors.MistSurface,
    onSurface = SagayColors.TextDark,
    surfaceVariant = SagayColors.CardWhite,
    onSurfaceVariant = SagayColors.TextMuted,
    outline = SagayColors.Hairline,
    error = SagayColors.WarningRed,
    onError = Color.White,
)

@Composable
fun SagayTheme(content: @Composable () -> Unit) {
    // The paper specifies a light theme; dark is intentionally not themed.
    isSystemInDarkTheme() // observed but ignored
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
