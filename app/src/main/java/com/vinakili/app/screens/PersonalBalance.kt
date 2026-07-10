package com.vinakili.app.screens

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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import com.vinakili.app.AppState
import com.vinakili.app.data.BalanceEntry
import com.vinakili.app.data.Repo
import com.vinakili.app.data.money
import com.vinakili.app.i18n.LocalStr
import com.vinakili.app.ui.AmountField
import com.vinakili.app.ui.Card
import com.vinakili.app.ui.DonutChart
import com.vinakili.app.ui.EmptyState
import com.vinakili.app.ui.Fab
import com.vinakili.app.ui.Field
import com.vinakili.app.ui.LocalB
import com.vinakili.app.ui.PrimaryButton
import com.vinakili.app.ui.SectionTitle
import com.vinakili.app.ui.Sheet
import com.vinakili.app.ui.Slice
import com.vinakili.app.ui.longPressable
import com.vinakili.app.ui.noRippleClickable
import com.vinakili.app.ui.toAmount

@Composable
fun BalanceScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val all by Repo.balances.flow.collectAsState()
    val entries = all.filter { it.deletedAt == null }

    var sheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BalanceEntry?>(null) }
    var where by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var colorIdx by remember { mutableStateOf(0) }

    fun colorForPlace(place: String, fallback: Int): Int {
        val existing = entries.firstOrNull { it.where.equals(place.trim(), true) }
        return existing?.color ?: fallback
    }

    val total = entries.sumOf { it.amount }
    val grouped = entries.groupBy { it.where.trim().lowercase() }
    val places = grouped.entries.map { (_, list) ->
        Triple(list.first().where, list.sumOf { it.amount }, list.first().color)
    }.sortedByDescending { it.second }

    Box(Modifier.fillMaxWidth()) {
        ScreenColumn {
            ScreenTitle(s.balance, sub = s.personal)

            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(s.totalBalance, color = b.textDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                com.vinakili.app.ui.AnimatedMoney(total, color = b.text, fontSize = 32.sp)
            }


            if (places.size >= 2) {
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    SectionTitle(s.distribution)
                    Spacer(Modifier.height(8.dp))
                    DonutChart(
                        places.map { Slice(it.first, it.second.toFloat(), b.palette[it.third % b.palette.size]) },
                        centerLabel = s.totalBalance, centerValue = com.vinakili.app.data.moneyCompact(total),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionTitle(s.balance)
            if (entries.isEmpty()) {
                EmptyState(s.emptyList, s.addSomething, Icons.Rounded.AccountBalanceWallet)
            } else {
                places.forEach { (place, sum, cIdx) ->
                    val first = entries.first { it.where.trim().equals(place.trim(), true) }
                    var actions by remember(first.id) { mutableStateOf(false) }
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(14.dp)).background(b.surface)
                            .border(1.dp, b.hairline, RoundedCornerShape(14.dp))
                            .longPressable(onClick = { actions = !actions }, onLongClick = { actions = true })
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(b.palette[cIdx % b.palette.size]))
                            Spacer(Modifier.width(12.dp))
                            Text(place.ifBlank { "—" }, color = b.text, fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text(money(sum), color = b.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        com.vinakili.app.ui.ExpandCard(actions) {
                            Spacer(Modifier.height(12.dp))
                            com.vinakili.app.ui.ActionChips(listOf(
                                com.vinakili.app.ui.RowAction(Icons.Rounded.Edit, s.edit, b.textDim) {
                                    editing = first; where = first.where; amount = first.amount.toString()
                                    colorIdx = first.color; sheet = true; actions = false
                                },
                                com.vinakili.app.ui.RowAction(Icons.Rounded.Delete, s.delete, b.danger) {
                                    Repo.balances.softDelete(first.id); app.showToast(s.deletedOk); actions = false
                                },
                            ))
                        }
                    }
                }
            }
        }
        Fab(onClick = { editing = null; where = ""; amount = ""; colorIdx = places.size % b.palette.size; sheet = true },
            accent = b.amber, onAccent = b.onAmber,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 92.dp))
    }

    Sheet(sheet, if (editing == null) s.add else s.edit, onDismiss = { sheet = false }) {
        Field(s.where, where, { where = it }, accent = b.amber)
        AmountField(s.amount, amount, { amount = it }, b.amber)
        Spacer(Modifier.height(8.dp))
        Text(s.color, color = b.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            b.palette.forEachIndexed { i, c ->
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(c)
                        .border(if (colorIdx == i) 3.dp else 0.dp, b.text, CircleShape)
                        .noRippleClickable { colorIdx = i },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(s.save, {
            if (where.isBlank() || amount.toAmount() <= 0) { app.showToast(s.amountRequired); return@PrimaryButton }
            val useColor = colorForPlace(where, colorIdx)
            val base = editing ?: BalanceEntry()
            Repo.balances.upsert(base.copy(where = where.trim(), amount = amount.toAmount(), color = useColor,
                updatedAt = com.vinakili.app.data.nowIso()))
            app.showToast(s.savedOk); sheet = false
        }, Modifier.fillMaxWidth(), accent = b.amber, onAccent = b.onAmber)
    }
}
