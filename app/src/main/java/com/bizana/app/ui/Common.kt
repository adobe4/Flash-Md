package com.bizana.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Card(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val b = LocalB.current
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(b.surface)
            .border(BorderStroke(1.dp, b.hairline), RoundedCornerShape(20.dp))
            .padding(padding)
    ) { Column { content() } }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val b = LocalB.current
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, color = b.textDim, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            letterSpacing = 0.6.sp)
        trailing?.invoke()
    }
}

@Composable
fun Pill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAccent: Color? = null,
) {
    val b = LocalB.current
    val bg by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "pill")
    val container = lerpColor(b.surface2, accent, bg)
    val fg = if (selected) (onAccent ?: b.onAmber) else b.textDim
    Box(
        modifier
            .clip(CircleShape)
            .background(container)
            .then(if (!selected) Modifier.border(1.dp, b.hairline, CircleShape) else Modifier)
            .noRippleClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)

@Composable
fun StatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    sub: String? = null,
    icon: ImageVector? = null,
) {
    val b = LocalB.current
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(b.surface)
            .border(1.dp, b.hairline, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(7.dp))
                Text(label, color = b.textDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, color = b.text, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(sub, color = b.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun StatusBadge(label: String, color: Color) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyState(title: String, sub: String? = null, icon: ImageVector? = null) {
    val b = LocalB.current
    Column(
        Modifier.fillMaxWidth().padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Box(Modifier.size(58.dp).clip(CircleShape).background(b.surface2),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = b.muted, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
        }
        Text(title, color = b.textDim, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        if (sub != null) {
            Spacer(Modifier.height(6.dp))
            Text(sub, color = b.muted, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = LocalB.current.amber,
    onAccent: Color = LocalB.current.onAmber,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val b = LocalB.current
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) accent else b.surface2)
            .noRippleClickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) onAccent else b.muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = if (enabled) onAccent else b.muted,
            fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun GhostButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val b = LocalB.current
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, b.hairline, RoundedCornerShape(14.dp))
            .background(b.surface2)
            .noRippleClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = b.textDim, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(label, color = b.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun Fab(onClick: () -> Unit, accent: Color, onAccent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(accent)
            .noRippleClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, "add", tint = onAccent, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun ExpandCard(expanded: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(180)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(tween(140)) + shrinkVertically(tween(160)),
    ) { Column { content() } }
}
