package com.bizana.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizana.app.ui.LocalB
import com.bizana.app.ui.noRippleClickable

@Composable
fun ToolChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    val b = LocalB.current
    Row(
        Modifier.clip(CircleShape).background(b.surface2).border(1.dp, b.hairline, CircleShape)
            .noRippleClickable(onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = b.textDim, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = b.textDim, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
fun IconAction(icon: ImageVector, tint: androidx.compose.ui.graphics.Color, desc: String, onClick: () -> Unit) {
    val b = LocalB.current
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(b.surface2)
            .noRippleClickable(onClick), contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(17.dp))
    }
}
