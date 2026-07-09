package com.vinakili.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.ReceiptLong
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
import com.vinakili.app.Screen
import com.vinakili.app.data.Repo
import com.vinakili.app.data.effectiveStatus
import com.vinakili.app.data.fmtDate
import com.vinakili.app.data.invoiceTotals
import com.vinakili.app.data.money
import com.vinakili.app.data.nowIso
import com.vinakili.app.i18n.LocalStr
import com.vinakili.app.i18n.statusLabel
import com.vinakili.app.ui.EmptyState
import com.vinakili.app.ui.ExpandCard
import com.vinakili.app.ui.Fab
import com.vinakili.app.ui.LocalB
import com.vinakili.app.ui.Pill
import com.vinakili.app.ui.StatusBadge
import com.vinakili.app.ui.longPressable
import com.vinakili.app.ui.noRippleClickable
import com.vinakili.app.ui.statusColor

@Composable
fun InvoicesScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val lang = Repo.lang.value ?: "en"
    val invoices by Repo.invoices.flow.collectAsState()
    val items by Repo.invoiceItems.flow.collectAsState()
    val payments by Repo.payments.flow.collectAsState()

    var filter by remember { mutableStateOf("all") }
    val filters = listOf("all" to s.all, "draft" to s.draft, "sent" to s.sent,
        "paid" to s.paid, "overdue" to s.overdue, "cancelled" to s.cancelled)

    val live = invoices.filter { it.deletedAt == null }.sortedByDescending { it.createdAt }
    val shown = live.filter { inv ->
        if (filter == "all") true
        else effectiveStatus(inv, invoiceTotals(inv, items, payments)) == filter
    }

    Box(Modifier.fillMaxWidth()) {
        ScreenColumn {
            ScreenTitle(s.invoices, sub = s.business)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { (k, label) ->
                    Pill(label, filter == k, b.blue, { filter = k }, onAccent = b.onBlue)
                }
            }
            Spacer(Modifier.height(14.dp))
            if (shown.isEmpty()) {
                EmptyState(s.emptyList, s.addSomething, Icons.Rounded.ReceiptLong)
            } else {
                shown.forEach { inv ->
                    val t = invoiceTotals(inv, items, payments)
                    val eff = effectiveStatus(inv, t)
                    var actions by remember(inv.id) { mutableStateOf(false) }
                    val overdue = eff == "overdue"
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp)).background(b.surface)
                            .border(1.dp, if (overdue) b.danger.copy(alpha = 0.5f) else b.hairline, RoundedCornerShape(16.dp))
                            .longPressable(onClick = { app.push(Screen.InvoiceDetail(inv.id)) },
                                onLongClick = { actions = !actions })
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(inv.number, color = b.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(inv.clientName.ifBlank { "—" }, color = b.textDim, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(money(t.total), color = b.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(Modifier.height(4.dp))
                                StatusBadge(s.statusLabel(eff), b.statusColor(eff))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("${s.dueDate}: ${fmtDate(inv.dueDate, lang)}",
                            color = if (overdue) b.danger else b.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        ExpandCard(actions) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (eff != "paid" && eff != "cancelled")
                                    IconAction(Icons.Rounded.Done, b.series[3], s.markPaid) {
                                        markPaid(inv.id); app.showToast(s.savedOk); actions = false
                                    }
                                IconAction(Icons.Rounded.ContentCopy, b.textDim, s.duplicate) {
                                    duplicateInvoice(inv.id); app.showToast(s.savedOk); actions = false
                                }
                                IconAction(Icons.Rounded.Delete, b.danger, s.delete) {
                                    Repo.invoices.softDelete(inv.id); app.showToast(s.deletedOk); actions = false
                                }
                            }
                        }
                    }
                }
            }
        }
        Fab(onClick = { app.push(Screen.InvoiceEditor(null)) }, accent = b.blue, onAccent = b.onBlue,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 92.dp))
    }
}

fun markPaid(invoiceId: String) {
    val inv = Repo.invoices.flow.value.firstOrNull { it.id == invoiceId } ?: return
    val t = invoiceTotals(inv, Repo.invoiceItems.flow.value, Repo.payments.flow.value)
    if (t.balance > 0) {
        Repo.payments.upsert(com.vinakili.app.data.Payment(invoiceId = invoiceId, amount = t.balance,
            date = com.vinakili.app.data.todayStr(), method = "cash"))
    }
    Repo.invoices.upsert(inv.copy(status = "paid",
        statusHistory = inv.statusHistory + com.vinakili.app.data.StatusChange("paid"), updatedAt = nowIso()))
}

fun duplicateInvoice(invoiceId: String) {
    val inv = Repo.invoices.flow.value.firstOrNull { it.id == invoiceId } ?: return
    val newId = com.vinakili.app.data.uuid()
    val copy = inv.copy(id = newId, number = Repo.nextInvoiceNumber(), status = "draft",
        statusHistory = emptyList(), date = com.vinakili.app.data.todayStr(),
        createdAt = nowIso(), updatedAt = nowIso(), deletedAt = null)
    Repo.invoices.upsert(copy)
    Repo.invoiceItems.flow.value.filter { it.deletedAt == null && it.invoiceId == invoiceId }.forEach {
        Repo.invoiceItems.upsert(it.copy(id = com.vinakili.app.data.uuid(), invoiceId = newId,
            createdAt = nowIso(), updatedAt = nowIso()))
    }
}
