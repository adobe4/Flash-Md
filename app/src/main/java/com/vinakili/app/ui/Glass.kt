package com.vinakili.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid-glass surface: soft shadow, translucent gradient fill, luminous edge.
 * Pass a [glow] color to tint the shadow (amber/blue halo).
 */
fun Modifier.liquidGlass(b: BColors, radius: Dp, glow: Color? = null, elevation: Dp = 14.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(elevation, shape,
            ambientColor = glow?.copy(alpha = 0.30f) ?: b.shadowTint,
            spotColor = glow?.copy(alpha = 0.45f) ?: b.shadowTint)
        .clip(shape)
        .background(Brush.linearGradient(listOf(b.glassHi, b.glassLo)))
        .border(1.dp, Brush.linearGradient(listOf(b.glassEdge, b.glassEdgeDim)), shape)
}

/** Solid card surface with a soft drop shadow. */
fun Modifier.softCard(b: BColors, radius: Dp, glow: Color? = null, elevation: Dp = 8.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(elevation, shape,
            ambientColor = glow?.copy(alpha = 0.25f) ?: b.shadowTint,
            spotColor = glow?.copy(alpha = 0.40f) ?: b.shadowTint)
        .clip(shape)
        .background(b.surface)
        .border(1.dp, b.hairline, shape)
}
