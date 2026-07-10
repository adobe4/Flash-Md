package com.vinakili.app.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Bars(val label: String, val values: List<Float>)

/** Grouped/simple animated bar chart. Series colors from theme. */
@Composable
fun BarChart(
    data: List<Bars>,
    seriesColors: List<Color>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 170.dp,
    valueLabel: ((Float) -> String)? = null,
) {
    val b = LocalB.current
    val anim by animateFloatAsState(if (data.isEmpty()) 0f else 1f, tween(420, easing = LinearOutSlowInEasing), label = "bars")
    val maxV = (data.flatMap { it.values }.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val density = LocalDensity.current
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val n = data.size
            if (n == 0) return@Canvas
            val groupW = size.width / n
            val seriesN = data.first().values.size.coerceAtLeast(1)
            val barGap = with(density) { 3.dp.toPx() }
            val innerW = groupW * 0.62f
            val barW = ((innerW - barGap * (seriesN - 1)) / seriesN).coerceAtLeast(2f)
            val baseY = size.height - with(density) { 18.dp.toPx() }
            // baseline
            drawLine(b.gridline, Offset(0f, baseY), Offset(size.width, baseY), strokeWidth = 1f)
            data.forEachIndexed { gi, group ->
                val gx = groupW * gi + (groupW - innerW) / 2f
                group.values.forEachIndexed { si, v ->
                    val h = (v / maxV) * (baseY - 6f) * anim
                    val x = gx + si * (barW + barGap)
                    val top = baseY - h
                    drawRoundRectCompat(
                        color = seriesColors[si % seriesColors.size],
                        topLeft = Offset(x, top),
                        size = Size(barW, h.coerceAtLeast(0f)),
                        radius = barW / 2.2f,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach {
                Text(it.label, color = b.muted, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
fun LineChart(
    values: List<Float>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 170.dp,
    area: Boolean = false,
    secondValues: List<Float>? = null,
    secondColor: Color? = null,
) {
    val b = LocalB.current
    val anim by animateFloatAsState(if (values.isEmpty()) 0f else 1f, tween(460, easing = LinearOutSlowInEasing), label = "line")
    val all = values + (secondValues ?: emptyList())
    val maxV = (all.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val density = LocalDensity.current
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val pad = with(density) { 16.dp.toPx() }
            val baseY = size.height - pad
            val topY = with(density) { 6.dp.toPx() }
            // gridlines
            for (i in 0..3) {
                val y = topY + (baseY - topY) * i / 3f
                drawLine(b.gridline, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            fun pts(vs: List<Float>): List<Offset> {
                if (vs.size < 2) {
                    if (vs.size == 1) return listOf(Offset(size.width / 2f, baseY - (vs[0] / maxV) * (baseY - topY) * anim))
                    return emptyList()
                }
                val stepX = size.width / (vs.size - 1)
                return vs.mapIndexed { i, v -> Offset(stepX * i, baseY - (v / maxV) * (baseY - topY) * anim) }
            }
            fun drawSeries(vs: List<Float>, c: Color, fill: Boolean) {
                val points = pts(vs)
                if (points.size < 2) return
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val p0 = points[i - 1]; val p1 = points[i]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }
                if (fill) {
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(points.last().x, baseY)
                        lineTo(points.first().x, baseY)
                        close()
                    }
                    drawPath(fillPath, Brush.verticalGradient(
                        listOf(c.copy(alpha = 0.30f), c.copy(alpha = 0.02f)),
                        startY = topY, endY = baseY))
                }
                drawPath(path, c, style = Stroke(width = with(density) { 2.5.dp.toPx() }))
                points.forEach { drawCircle(c, with(density) { 3.dp.toPx() }, it) }
            }
            secondValues?.let { drawSeries(it, secondColor ?: b.seriesNeutral, area) }
            drawSeries(values, color, area)
        }
        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach {
                    Text(it, color = b.muted, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

data class Slice(val label: String, val value: Float, val color: Color)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutChart(
    slices: List<Slice>,
    modifier: Modifier = Modifier,
    centerLabel: String? = null,
    centerValue: String? = null,
) {
    val b = LocalB.current
    val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    val anim by animateFloatAsState(if (slices.isEmpty()) 0f else 1f, tween(480, easing = LinearOutSlowInEasing), label = "donut")
    val density = LocalDensity.current
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(180.dp)) {
                val stroke = with(density) { 26.dp.toPx() }
                val inset = stroke / 2f + 2f
                var start = -90f
                slices.forEach { s ->
                    val sweep = (s.value / total) * 360f * anim
                    drawArc(
                        color = s.color,
                        startAngle = start,
                        sweepAngle = sweep - 2f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                    start += sweep
                }
            }
            if (centerValue != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(centerValue, color = b.text, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    if (centerLabel != null)
                        Text(centerLabel, color = b.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            slices.forEach { s ->
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(s.color))
                    Spacer(Modifier.width(6.dp))
                    Text(s.label, color = b.textDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, accent: Color, modifier: Modifier = Modifier, track: Color = LocalB.current.surface2) {
    val anim by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
        label = "prog")
    Box(
        modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(track)
    ) {
        Box(Modifier.fillMaxWidth(anim.coerceIn(0f, 1f)).height(9.dp).clip(CircleShape).background(accent))
    }
}

private fun DrawScope.drawRoundRectCompat(color: Color, topLeft: Offset, size: Size, radius: Float) {
    if (size.height <= 0f) return
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
    )
}
