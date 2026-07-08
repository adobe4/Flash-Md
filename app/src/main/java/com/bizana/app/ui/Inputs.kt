package com.bizana.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Field(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    accent: Color = LocalB.current.amber,
) {
    val b = LocalB.current
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (label.isNotEmpty()) {
            Text(label, color = b.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 5.dp))
        }
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(b.surface2)
                .border(1.dp, b.hairline, RoundedCornerShape(13.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(placeholder, color = b.muted, fontSize = 15.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = singleLine,
                textStyle = TextStyle(color = b.text, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                modifier = Modifier.fillMaxWidth().heightIn(min = if (singleLine) 0.dp else 60.dp),
            )
        }
    }
}

@Composable
fun AmountField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
) {
    Field(label, value, { onValue(it.filter { c -> c.isDigit() || c == '.' }) },
        modifier, placeholder, KeyboardType.Decimal, accent = accent)
}

fun String.toAmount(): Double = this.filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull() ?: 0.0

@Composable
fun <T> SegPicker(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
) {
    val b = LocalB.current
    Row(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(b.surface2)
            .border(1.dp, b.hairline, RoundedCornerShape(13.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for ((value, label) in options) {
            val sel = value == selected
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) accent else Color.Transparent)
                    .noRippleClickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (sel) onAccent else b.textDim,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun <T> Dropdown(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val b = LocalB.current
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }?.second ?: ""
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (label.isNotEmpty()) {
            Text(label, color = b.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 5.dp))
        }
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(b.surface2)
                .border(1.dp, b.hairline, RoundedCornerShape(13.dp))
                .noRippleClickable { open = !open }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(current, color = b.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = b.textDim)
        }
        ExpandCard(open) {
            Column(
                Modifier.fillMaxWidth().padding(top = 6.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(b.surface2)
                    .border(1.dp, b.hairline, RoundedCornerShape(13.dp))
            ) {
                for ((value, text) in options) {
                    Row(
                        Modifier.fillMaxWidth()
                            .noRippleClickable { onSelect(value); open = false }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val sel = value == selected
                        Box(Modifier.size(7.dp).clip(CircleShape)
                            .background(if (sel) accent else b.hairline))
                        Spacer(Modifier.width(10.dp))
                        Text(text, color = if (sel) b.text else b.textDim,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
