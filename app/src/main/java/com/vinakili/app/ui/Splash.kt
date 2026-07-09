package com.vinakili.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * Animated intro: a glowing halo behind the V+pen mark that springs in,
 * with the wordmark and tagline rising underneath.
 */
@Composable
fun SplashScreen() {
    val b = LocalB.current
    var start by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { start = true }

    val markScale by animateFloatAsState(
        if (start) 1f else 0.55f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "markScale",
    )
    val markAlpha by animateFloatAsState(if (start) 1f else 0f, tween(500), label = "markAlpha")
    val textAlpha by animateFloatAsState(if (start) 1f else 0f, tween(650, delayMillis = 260), label = "textAlpha")
    val textRise by animateFloatAsState(if (start) 0f else 26f, tween(650, delayMillis = 260), label = "textRise")

    val halo = rememberInfiniteTransition(label = "halo")
    val haloPulse by halo.animateFloat(
        initialValue = 0.85f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "haloPulse",
    )
    val ringSpin by halo.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Restart), label = "ringSpin",
    )

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                listOf(b.surface, b.bg),
                center = Offset.Unspecified,
                radius = 1400f,
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        // amber glow halo
        Box(
            Modifier.size(240.dp).scale(haloPulse).graphicsLayer { alpha = 0.5f * markAlpha }
                .background(
                    Brush.radialGradient(listOf(b.amber.copy(alpha = 0.35f), b.amber.copy(alpha = 0f)))
                )
        )
        // slow-spinning dashed accent ring
        Box(Modifier.size(150.dp).rotate(ringSpin).graphicsLayer { alpha = 0.6f * markAlpha }) {
            DashRing(b.blue.copy(alpha = 0.5f))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(108.dp).scale(markScale).graphicsLayer { alpha = markAlpha }
                    .liquidGlass(b, 30.dp, glow = b.amber),
                contentAlignment = Alignment.Center,
            ) {
                VinakiliMark(70.dp)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "Vinakili",
                color = b.text, fontWeight = FontWeight.Black, fontSize = 30.sp,
                modifier = Modifier.graphicsLayer { alpha = textAlpha; translationY = textRise },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Fedha · Ankara",
                color = b.textDim, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = textAlpha * 0.9f; translationY = textRise },
            )
        }
    }
}

@Composable
private fun DashRing(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val stroke = size.minDimension * 0.02f
        drawCircle(
            color = color,
            radius = size.minDimension / 2f - stroke,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(size.minDimension * 0.05f, size.minDimension * 0.06f), 0f),
            ),
        )
    }
}
