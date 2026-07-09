package com.vinakili.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp

/** The Vinakili mark: a two-tone V with a pen after it. */
@Composable
fun VinakiliMark(size: Dp, modifier: Modifier = Modifier) {
    val b = LocalB.current
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.13f
        // V — left arm amber, right arm blue
        drawLine(b.amber, Offset(w * 0.08f, h * 0.24f), Offset(w * 0.32f, h * 0.80f),
            strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(b.blue, Offset(w * 0.32f, h * 0.80f), Offset(w * 0.56f, h * 0.24f),
            strokeWidth = stroke, cap = StrokeCap.Round)
        // pen body
        drawLine(b.text, Offset(w * 0.70f, h * 0.60f), Offset(w * 0.92f, h * 0.22f),
            strokeWidth = stroke * 0.85f, cap = StrokeCap.Round)
        // pen nib
        val nib = Path().apply {
            moveTo(w * 0.58f, h * 0.84f)
            lineTo(w * 0.655f, h * 0.545f)
            lineTo(w * 0.775f, h * 0.615f)
            close()
        }
        drawPath(nib, b.amber)
    }
}
