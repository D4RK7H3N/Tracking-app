package com.sagay.touristnav.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Branded launch screen shown right after the system splash: sunrise gradient,
 * a route drawing itself to a dropping pin, then the app name. Auto-finishes
 * after ~1.9s; a tap skips it. All animation is presentation-only.
 */
@Composable
fun LaunchScreen(onDone: () -> Unit) {
    var finished by remember { mutableStateOf(false) }
    fun finish() {
        if (!finished) {
            finished = true
            onDone()
        }
    }

    val routeProgress = remember { Animatable(0f) }
    val pinProgress = remember { Animatable(0f) }
    val textProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { routeProgress.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) }
        delay(800)
        launch {
            pinProgress.animateTo(
                1f,
                spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
            )
        }
        delay(450)
        launch { textProgress.animateTo(1f, tween(400)) }
        delay(900)
        finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SagayColors.sunriseGradient)
            .pointerInput(Unit) { detectTapGestures { finish() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RouteToPinAnimation(
                routeProgress = routeProgress.value,
                pinProgress = pinProgress.value,
                modifier = Modifier.size(width = 240.dp, height = 170.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Sagay Tourist Navigator",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.graphicsLayer {
                    alpha = textProgress.value.coerceIn(0f, 1f)
                    translationY = (1f - textProgress.value.coerceIn(0f, 1f)) * 24f
                },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your ride around Sagay City",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.graphicsLayer {
                    alpha = textProgress.value.coerceIn(0f, 1f)
                    translationY = (1f - textProgress.value.coerceIn(0f, 1f)) * 24f
                },
            )
        }
    }
}

@Composable
private fun RouteToPinAnimation(
    routeProgress: Float,
    pinProgress: Float,
    modifier: Modifier = Modifier,
) {
    val routePath = remember {
        Path().apply {
            moveTo(16f, 138f)
            cubicTo(66f, 132f, 68f, 66f, 112f, 68f)
            cubicTo(156f, 70f, 196f, 82f, 212f, 30f)
        }
    }
    val pathMeasure = remember {
        PathMeasure().also { it.setPath(routePath, false) }
    }
    val pinColor = Color(0xFFFFF59D)

    Canvas(modifier) {
        val total = pathMeasure.length
        val drawn = Path()
        pathMeasure.getSegment(0f, total * routeProgress.coerceIn(0f, 1f), drawn, true)
        drawPath(
            drawn,
            color = Color.White,
            style = Stroke(width = 6f, cap = StrokeCap.Round),
        )

        val endPoint = pathMeasure.getPosition(total)
        val drop = (1f - pinProgress.coerceIn(0f, 1f)) * -60f
        val alpha = pinProgress.coerceIn(0f, 1f)
        val pinCenter = androidx.compose.ui.geometry.Offset(endPoint.x, endPoint.y + drop)

        drawCircle(
            color = Color.White.copy(alpha = 0.35f * alpha),
            radius = 22f,
            center = pinCenter,
        )
        val tail = Path().apply {
            moveTo(pinCenter.x - 9f, pinCenter.y + 3f)
            lineTo(pinCenter.x + 9f, pinCenter.y + 3f)
            lineTo(pinCenter.x, pinCenter.y + 18f)
            close()
        }
        drawPath(tail, color = pinColor, alpha = alpha)
        drawCircle(color = pinColor, radius = 11f, center = pinCenter, alpha = alpha)
        drawCircle(color = SagayColors.SunriseDeep, radius = 4.5f, center = pinCenter, alpha = alpha)
    }
}
