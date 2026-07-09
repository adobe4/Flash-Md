package com.vinakili.app.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinakili.app.ui.LocalB
import com.vinakili.app.ui.VinakiliMark
import com.vinakili.app.ui.liquidGlass
import com.vinakili.app.ui.noRippleClickable

@Composable
fun LanguagePicker(onPick: (String) -> Unit) {
    val b = LocalB.current
    val transition = rememberInfiniteTransition(label = "logo")
    val pulse by transition.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "pulse",
    )
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(b.bg, b.surface)))
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(112.dp).scale(pulse).liquidGlass(b, 32.dp, glow = b.amber),
            contentAlignment = Alignment.Center,
        ) {
            VinakiliMark(72.dp)
        }
        Spacer(Modifier.height(24.dp))
        Text("Vinakili", color = b.text, fontWeight = FontWeight.Black, fontSize = 34.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            "Choose your language / Chagua lugha yako",
            color = b.textDim, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(44.dp))
        LangButton("English", "🇬🇧") { onPick("en") }
        Spacer(Modifier.height(14.dp))
        LangButton("Kiswahili", "🇹🇿") { onPick("sw") }
    }
}

@Composable
private fun LangButton(label: String, flag: String, onClick: () -> Unit) {
    val b = LocalB.current
    Box(
        Modifier.fillMaxWidth().clip(CircleShape).background(b.surface2)
            .noRippleClickable(onClick).padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("$flag   $label", color = b.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
