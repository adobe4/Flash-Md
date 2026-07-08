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
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizana.app.AppState
import com.bizana.app.data.PersonalItem
import com.bizana.app.data.Repo
import com.bizana.app.data.money
import com.bizana.app.i18n.LocalStr
import com.bizana.app.ui.AmountField
import com.bizana.app.ui.Card
import com.bizana.app.ui.EmptyState
import com.bizana.app.ui.Fab
import com.bizana.app.ui.Field
import com.bizana.app.ui.LocalB
import com.bizana.app.ui.PrimaryButton
import com.bizana.app.ui.SegPicker
import com.bizana.app.ui.Sheet
import com.bizana.app.ui.ConfirmDialog
import com.bizana.app.ui.longPressable
import com.bizana.app.ui.noRippleClickable
import com.bizana.app.ui.toAmount

@Composable
fun ListsScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val all by Repo.personalItems.flow.collectAsState()
    val items = all.filter { it.deletedAt == null && it.type != "spend" }

    var sheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PersonalItem?>(null) }
    var typeSel by remember { mutableStateOf("debt") }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    fun openAdd(type: String) {
        editing = null; typeSel = type; name = ""; amount = ""; sheet = true
    }
    fun openEdit(it: PersonalItem) {
        editing = it; typeSel = it.type; name = it.name; amount = if (it.amount == 0.0) "" else it.amount.toString(); sheet = true
    }

    val active = items.filter { !it.done }
    val debtTotal = active.filter { it.type == "debt" }.sumOf { it.amount }
    val buyTotal = active.filter { it.type == "buy" }.sumOf { it.amount }
    val doneCount = items.count { it.done }

    Box(Modifier.fillMaxWidth()) {
        ScreenColumn {
            ScreenTitle(s.lists, sub = s.personal)

            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(s.outstanding, color = b.textDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(money(debtTotal + buyTotal), color = b.text, fontWeight = FontWeight.Black, fontSize = 30.sp)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat(s.debt, money(debtTotal), b.amber, Modifier.weight(1f))
                    MiniStat(s.toBuy, money(buyTotal), b.blue, Modifier.weight(1f))
                    MiniStat(s.doneCount, doneCount.toString(), b.muted, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolChip(s.exportJson, Icons.Rounded.Download) {
                    app.share(com.bizana.app.OutFile("bizana-lists.json", "application/json",
                        text = Repo.personalItems.encode()))
                }
                ToolChip(s.exportCsv, Icons.Rounded.Download) {
                    app.share(com.bizana.app.OutFile("bizana-lists.csv", "text/csv", text = Repo.listsCsv()))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolChip(s.importW, Icons.Rounded.Upload) {
                    app.importCsv { raw ->
                        val n = try {
                            if (raw.trimStart().startsWith("[")) Repo.personalItems.decodeAndMerge(raw)
                            else Repo.importListsCsv(raw)
                        } catch (_: Exception) { -1 }
                        app.showToast(if (n >= 0) "$n ${s.itemsLoaded}" else s.importFailed)
                    }
                }
                ToolChip(s.clearAll, Icons.Rounded.DeleteSweep) { confirmClear = true }
            }

            Spacer(Modifier.height(14.dp))
            if (items.isEmpty()) {
                EmptyState(s.emptyList, s.addSomething, Icons.Rounded.Check)
            } else {
                items.sortedWith(compareBy({ it.done }, { it.type })).forEach { item ->
                    ListRow(item,
                        onToggle = { Repo.personalItems.upsert(item.copy(done = !item.done, updatedAt = com.bizana.app.data.nowIso())) },
                        onEdit = { openEdit(item) },
                        onDelete = { Repo.personalItems.softDelete(item.id); app.showToast(s.deletedOk) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Fab(onClick = { openAdd("debt") }, accent = b.amber, onAccent = b.onAmber,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp))
    }

    Sheet(sheet, if (editing == null) s.add else s.edit, onDismiss = { sheet = false }) {
        SegPicker(listOf("debt" to s.debt, "buy" to s.toBuy), typeSel, { typeSel = it }, b.amber, b.onAmber)
        Field(s.name, name, { name = it }, accent = b.amber)
        AmountField(s.amount, amount, { amount = it }, b.amber)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(s.save, {
            if (name.isBlank() || amount.toAmount() <= 0) { app.showToast(s.amountRequired); return@PrimaryButton }
            val base = editing ?: PersonalItem(type = typeSel)
            Repo.personalItems.upsert(base.copy(type = typeSel, name = name.trim(), amount = amount.toAmount(),
                updatedAt = com.bizana.app.data.nowIso()))
            app.showToast(s.savedOk); sheet = false
        }, Modifier.fillMaxWidth(), accent = b.amber, onAccent = b.onAmber)
    }

    ConfirmDialog(confirmClear, s.confirmClear, s.clearAll,
        onConfirm = { items.forEach { Repo.personalItems.softDelete(it.id) }; app.showToast(s.deletedOk) },
        onDismiss = { confirmClear = false })
}

@Composable
fun MiniStat(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val b = LocalB.current
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(b.surface2).padding(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(5.dp))
            Text(label, color = b.textDim, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, color = b.text, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun ListRow(item: PersonalItem, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val b = LocalB.current
    val s = LocalStr.current
    var actions by remember { mutableStateOf(false) }
    val accent = if (item.type == "debt") b.amber else b.blue
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(b.surface)
            .border(1.dp, b.hairline, RoundedCornerShape(16.dp))
            .longPressable(onClick = { actions = !actions }, onLongClick = { actions = true })
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(24.dp).clip(CircleShape)
                    .background(if (item.done) accent else b.surface2)
                    .border(1.dp, if (item.done) accent else b.hairline, CircleShape)
                    .noRippleClickable(onToggle),
                contentAlignment = Alignment.Center,
            ) {
                if (item.done) Icon(Icons.Rounded.Check, null,
                    tint = if (item.type == "debt") b.onAmber else b.onBlue, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name.ifBlank { "—" }, color = if (item.done) b.muted else b.text,
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null)
                Text(if (item.type == "debt") s.debt else s.toBuy, color = accent,
                    fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Text(money(item.amount), color = if (item.done) b.muted else b.text,
                fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        com.bizana.app.ui.ExpandCard(actions) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconAction(Icons.Rounded.Edit, b.textDim, s.edit, onEdit)
                IconAction(Icons.Rounded.Delete, b.danger, s.delete, onDelete)
            }
        }
    }
}
