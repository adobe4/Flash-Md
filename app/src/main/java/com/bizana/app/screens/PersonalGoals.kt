package com.bizana.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizana.app.AppState
import com.bizana.app.data.Goal
import com.bizana.app.data.Repo
import com.bizana.app.data.money
import com.bizana.app.data.moneyCompact
import com.bizana.app.data.nowIso
import com.bizana.app.i18n.LocalStr
import com.bizana.app.ui.AmountField
import com.bizana.app.ui.Card
import com.bizana.app.ui.EmptyState
import com.bizana.app.ui.Fab
import com.bizana.app.ui.Field
import com.bizana.app.ui.LocalB
import com.bizana.app.ui.PrimaryButton
import com.bizana.app.ui.ProgressBar
import com.bizana.app.ui.SectionTitle
import com.bizana.app.ui.SegPicker
import com.bizana.app.ui.Sheet
import com.bizana.app.ui.longPressable
import com.bizana.app.ui.noRippleClickable
import com.bizana.app.ui.toAmount

@Composable
fun GoalsScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val all by Repo.goals.flow.collectAsState()
    val goals = all.filter { it.deletedAt == null }

    var sheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Goal?>(null) }
    var kind by remember { mutableStateOf("money") }
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }

    val moneyGoals = goals.filter { it.kind == "money" }
    val stuff = goals.filter { it.kind == "stuff" }
    val savedTotal = moneyGoals.sumOf { it.current }
    val targetTotal = moneyGoals.sumOf { it.target }
    val wishlistTotal = stuff.filter { !it.done }.sumOf { it.price }

    Box(Modifier.fillMaxWidth()) {
        ScreenColumn {
            ScreenTitle(s.goals, sub = s.personal)
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat(s.savedVsTarget, money(savedTotal), b.amber, Modifier.weight(1f))
                    MiniStat(s.wishlistTotal, money(wishlistTotal), b.blue, Modifier.weight(1f))
                }
                if (targetTotal > 0) {
                    Spacer(Modifier.height(12.dp))
                    ProgressBar((savedTotal / targetTotal).toFloat(), b.amber)
                }
            }

            if (moneyGoals.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionTitle(s.moneyGoal)
                moneyGoals.forEach { g -> MoneyGoalCard(g, app) }
            }
            if (stuff.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionTitle(s.stuffToBuy)
                stuff.forEach { g -> StuffCard(g, app) }
            }
            if (goals.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                EmptyState(s.emptyList, s.addSomething, Icons.Rounded.EmojiEvents)
            }
        }
        Fab(onClick = { editing = null; kind = "money"; name = ""; target = ""; current = ""; price = ""; due = ""; sheet = true },
            accent = b.amber, onAccent = b.onAmber,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp))
    }

    Sheet(sheet, if (editing == null) s.add else s.edit, onDismiss = { sheet = false }) {
        SegPicker(listOf("money" to s.moneyGoal, "stuff" to s.stuffToBuy), kind, { kind = it }, b.amber, b.onAmber)
        Field(s.name, name, { name = it }, accent = b.amber)
        if (kind == "money") {
            AmountField(s.target, target, { target = it }, b.amber)
            AmountField(s.saved, current, { current = it }, b.amber)
        } else {
            AmountField(s.approxPrice, price, { price = it }, b.amber)
            Field(s.dueMonth, due, { due = it }, placeholder = "2026-08", accent = b.amber)
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(s.save, {
            if (name.isBlank()) { app.showToast(s.amountRequired); return@PrimaryButton }
            val base = editing ?: Goal(kind = kind)
            val g = if (kind == "money")
                base.copy(kind = "money", name = name.trim(), target = target.toAmount(),
                    current = current.toAmount(), done = current.toAmount() >= target.toAmount() && target.toAmount() > 0,
                    updatedAt = nowIso())
            else
                base.copy(kind = "stuff", name = name.trim(), price = price.toAmount(), dueMonth = due.trim(), updatedAt = nowIso())
            Repo.goals.upsert(g)
            app.showToast(s.savedOk); sheet = false
        }, Modifier.fillMaxWidth(), accent = b.amber, onAccent = b.onAmber)
    }
}

@Composable
private fun MoneyGoalCard(g: Goal, app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val frac = if (g.target > 0) (g.current / g.target).toFloat().coerceIn(0f, 1f) else 0f
    val done = g.current >= g.target && g.target > 0
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(Modifier.fillMaxWidth().longPressable(onClick = {}, onLongClick = { Repo.goals.softDelete(g.id); app.showToast(s.deletedOk) })) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(g.name.ifBlank { "—" }, color = b.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (done) Text(s.goalReached, color = b.amber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text("${money(g.current)} / ${money(g.target)}", color = b.textDim, fontSize = 13.sp,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                ProgressBar(frac, b.amber)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAdd("+1k", 1000.0, g)
                    QuickAdd("+10k", 10000.0, g)
                    QuickAdd("+100k", 100000.0, g)
                }
            }
        }
    }
}

@Composable
private fun QuickAdd(label: String, amt: Double, g: Goal) {
    val b = LocalB.current
    Box(
        Modifier.clip(CircleShape).background(b.amber.copy(alpha = 0.16f))
            .noRippleClickable {
                Repo.goals.upsert(g.copy(current = g.current + amt,
                    done = (g.current + amt) >= g.target && g.target > 0, updatedAt = nowIso()))
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = b.amber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun StuffCard(g: Goal, app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth()
            .longPressable(onClick = {}, onLongClick = { Repo.goals.softDelete(g.id); app.showToast(s.deletedOk) }),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(g.name.ifBlank { "—" }, color = if (g.done) b.muted else b.text,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(money(g.price) + (if (g.dueMonth.isNotBlank()) " · ${g.dueMonth}" else ""),
                    color = b.textDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                Modifier.clip(CircleShape)
                    .background(if (g.done) b.blue else b.surface2)
                    .noRippleClickable { Repo.goals.upsert(g.copy(done = !g.done, updatedAt = nowIso())) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, tint = if (g.done) b.onBlue else b.textDim, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(s.markBought, color = if (g.done) b.onBlue else b.textDim,
                        fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}
