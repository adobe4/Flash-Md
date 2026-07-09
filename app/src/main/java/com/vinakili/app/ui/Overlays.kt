package com.vinakili.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinakili.app.i18n.LocalStr

@Composable
fun Sheet(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val b = LocalB.current
    AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
        Box(
            Modifier.fillMaxSize().background(b.scrim).noRippleClickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter,
        ) { }
    }
    AnimatedVisibility(
        visible,
        enter = slideInVertically(spring(dampingRatio = 0.82f, stiffness = 420f)) { it } + fadeIn(tween(180)),
        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(140)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(b.surface)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 18.dp)
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape).background(b.hairline))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, color = b.text, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                    Box(Modifier.size(32.dp).clip(CircleShape).background(b.surface2)
                        .noRippleClickable { onDismiss() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, "close", tint = b.textDim, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.size(8.dp))
                Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    visible: Boolean,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val b = LocalB.current
    val s = LocalStr.current
    AnimatedVisibility(visible, enter = fadeIn(tween(150)), exit = fadeOut(tween(120))) {
        Box(Modifier.fillMaxSize().background(b.scrim).noRippleClickable { onDismiss() },
            contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(36.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)).background(b.surface).padding(22.dp)
            ) {
                Text(message, color = b.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.size(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(s.cancel, onDismiss, Modifier.weight(1f))
                    PrimaryButton(confirmLabel, { onConfirm(); onDismiss() }, Modifier.weight(1f),
                        accent = b.danger, onAccent = b.onDanger)
                }
            }
        }
    }
}

@Composable
fun BoxScopeToastHost(message: String?, modifier: Modifier = Modifier) {
    val b = LocalB.current
    AnimatedVisibility(
        message != null,
        enter = slideInVertically(tween(240)) { -it } + fadeIn(),
        exit = slideOutVertically(tween(200)) { -it } + fadeOut(),
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(b.text)
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Text(message ?: "", color = b.bg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}
