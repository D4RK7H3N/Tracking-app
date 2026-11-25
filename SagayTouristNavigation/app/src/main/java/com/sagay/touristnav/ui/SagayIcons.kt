package com.sagay.touristnav.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sagay.touristnav.R
import com.sagay.touristnav.domain.models.VehicleType

/**
 * Icon building blocks for the trip result card, the destination list and the
 * terminal legend.
 *
 * All glyphs are local vector drawables (see tools/extract-material-icons.ps1),
 * so the app ships no icon dependency. Route colour is applied at runtime
 * rather than baked into a drawable per colour, which is why the red, yellow
 * and white services all share one silhouette.
 */
object SagayIcons {

    /** Drawable for each vehicle type. */
    fun drawableFor(vehicle: VehicleType): Int = when (vehicle) {
        VehicleType.TRICYCLE -> R.drawable.ic_electric_rickshaw
        VehicleType.REPORM_2 -> R.drawable.ic_tricycle_reporm2
        VehicleType.BOAT -> R.drawable.ic_directions_boat
    }
}

/**
 * The vehicle glyph for [vehicle], tinted with [tint].
 *
 * [contentDescription] defaults to the vehicle's display name so the icon is
 * announced correctly by TalkBack.
 */
@Composable
fun VehicleIcon(
    vehicle: VehicleType,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = vehicle.displayName,
) {
    Icon(
        painter = painterResource(SagayIcons.drawableFor(vehicle)),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

/**
 * Circular badge showing the route colour with the vehicle glyph inside it.
 *
 * The glyph is drawn in an ink that contrasts with the route colour, because a
 * white or yellow route would otherwise be unreadable on the white card
 * surface. Very light route colours also get a hairline border so the badge
 * still reads as a white (or pale) circle.
 */
@Composable
fun RouteBadge(
    vehicle: VehicleType,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val routeColor = Color(parseHexColor(colorHex))
    val ink = contrastingInk(routeColor)
    val needsBorder = routeColor.luminance() > 0.9f
    Box(
        modifier = modifier
            .size(size)
            .background(routeColor, CircleShape)
            .then(
                if (needsBorder) {
                    Modifier.border(1.dp, SagayColors.Hairline, CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        VehicleIcon(
            vehicle = vehicle,
            modifier = Modifier.size(size * 0.58f),
            tint = ink,
        )
    }
}

/**
 * Picks the glyph colour that is readable on [background].
 *
 * A route can be white or yellow, which are unreadable under a white glyph,
 * so the ink is chosen by WCAG contrast ratio rather than by eye: white wins
 * on dark routes, near-black on light ones.
 */
private fun contrastingInk(background: Color): Color {
    val white = Color.White
    val dark = SagayColors.TextDark
    return if (contrastRatio(background, white) >= contrastRatio(background, dark)) {
        white
    } else {
        dark
    }
}

/** WCAG relative luminance. */
private fun relativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(color.red) +
        0.7152 * channel(color.green) +
        0.0722 * channel(color.blue)
}

/** WCAG contrast ratio, from 1 (identical) to 21 (black on white). */
private fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val lighter = maxOf(la, lb)
    val darker = minOf(la, lb)
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * One line of the result card: an icon, a label and a value.
 *
 * [value] is optional; when omitted the row renders as a single line of text.
 */
@Composable
fun IconDetailRow(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            )
        }
    }
}
