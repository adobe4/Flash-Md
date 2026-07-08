package com.bizana.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.bizana.app.ui.LocalB
import com.bizana.app.ui.noRippleClickable

/** Scrollable screen body with consistent side padding and space for the floating nav. */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    bottomSpace: Int = 96,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).padding(top = 4.dp),
    ) {
        content()
        Spacer(Modifier.height(bottomSpace.dp))
    }
}

@Composable
fun ScreenTitle(title: String, sub: String? = null, trailing: (@Composable () -> Unit)? = null) {
    val b = LocalB.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = b.text, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            if (sub != null) Text(sub, color = b.textDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        trailing?.invoke()
    }
}

/** Full-height overlay screen (settings, detail, editor) with a back bar. */
@Composable
fun OverlayScreen(
    title: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val b = LocalB.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(b.surface2)
                    .border(1.dp, b.hairline, CircleShape).noRippleClickable(onBack),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, "back", tint = b.text, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(title, color = b.text, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            trailing?.invoke()
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            content()
            Spacer(Modifier.height(40.dp))
        }
    }
}
