package com.vinakili.app.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext

/** Clickable with no ripple + a fluid springy press-down scale. No haptics on plain taps. */
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = noRippleClickable(true, onClick)

fun Modifier.noRippleClickable(enabled: Boolean, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    val press = androidx.compose.foundation.interaction.PressInteraction.Press(it)
                    interaction.emit(press)
                    tryAwaitRelease()
                    interaction.emit(androidx.compose.foundation.interaction.PressInteraction.Release(press))
                },
                onTap = { onClick() },
            )
        }
}

/** Tap acts normally; long-press gets a single confirming buzz — the only routine haptic. */
fun Modifier.longPressable(onClick: () -> Unit, onLongClick: () -> Unit): Modifier = composed {
    val context = LocalContext.current
    this.pointerInput(Unit) {
        detectTapGestures(
            onTap = { onClick() },
            onLongPress = { Haptics.buzz(context); onLongClick() },
        )
    }
}

object Haptics {
    private fun vib(ctx: android.content.Context): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }
    } catch (_: Exception) { null }

    fun buzz(ctx: android.content.Context) = pulse(ctx, 24)

    private fun pulse(ctx: android.content.Context, ms: Long) {
        val v = vib(ctx) ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v.vibrate(ms)
            }
        } catch (_: Exception) { }
    }
}
