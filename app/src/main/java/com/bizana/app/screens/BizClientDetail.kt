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
import androidx.compose.material.icons.rounded.Add
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
import com.bizana.app.AppState
import com.bizana.app.Screen
import com.bizana.app.data.Client
import com.bizana.app.data.Repo
import com.bizana.app.data.effectiveStatus
import com.bizana.app.data.fmtDate
import com.bizana.app.data.invoiceTotals
import com.bizana.app.data.money
import com.bizana.app.data.nowIso
import com.bizana.app.i18n.LocalStr
import com.bizana.app.i18n.methodLabel
import com.bizana.app.i18n.statusLabel
import com.bizana.app.ui.Card
import com.bizana.app.ui.ConfirmDialog
import com.bizana.app.ui.Field
import com.bizana.app.ui.LocalB
import com.bizana.app.ui.PrimaryButton
import com.bizana.app.ui.Sheet
import com.bizana.app.ui.StatusBadge
import com.bizana.app.ui.noRippleClickable
import com.bizana.app.ui.statusColor

@Composable
fun ClientDetailScreen(app: AppState, clientId: String) {
    val b = LocalB.current
    val s = LocalStr.current
    val lang = Repo.lang.value ?: "en"
    val clients by Repo.clients.flow.collectAsState()
    val invoices by Repo.invoices.flow.collectAsState()
    val items by Repo.invoiceItems.flow.collectAsState()
    val payments by Repo.payments.flow.collectAsState()

    val client = clients.firstOrNull { it.id == clientId }
    if (client == null) { app.pop(); return }

    var edit by remember { mutableStateOf(false) }
    var confirmDel by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(client.name) }
    var phone by remember { mutableStateOf(client.phone) }
    var email by remember { mutableStateOf(client.email) }
    var address by remember { mutableStateOf(client.address) }
    var notes by remember { mutableStateOf(client.notes) }

    val invd = invoices.filter { it.deletedAt == null && it.clientId == clientId }.sortedByDescending { it.createdAt }
    val nonCancelled = invd.filter { it.status != "cancelled" }
    val lifetime = nonCancelled.sumOf { invoiceTotals(it, items, payments).total }
    val paid = nonCancelled.sumOf { invoiceTotals(it, items, payments).paid }
    val clientPayments = payments.filter { p -> p.deletedAt == null && invd.any { it.id == p.invoiceId } }
        .sortedByDescending { it.date }

    OverlayScreen(client.name, onBack = { app.pop() }, trailing = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconAction(Icons.Rounded.Edit, b.textDim, s.edit) { edit = true }
            IconAction(Icons.Rounded.Delete, b.danger, s.delete) { confirmDel = true }
        }
    }) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(s.lifetimeInvoiced, money(lifetime), b.blue, Modifier.weight(1f))
            MiniStat(s.totalPaid, money(paid), b.series[3], Modifier.weight(1f))
            MiniStat(s.balanceOwed, money(lifetime - paid), b.amber, Modifier.weight(1f))
        }
        if (client.phone.isNotBlank() || client.email.isNotBlank() || client.address.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                if (client.phone.isNotBlank()) InfoRow(s.phone, client.phone)
                if (client.email.isNotBlank()) InfoRow(s.email, client.email)
                if (client.address.isNotBlank()) InfoRow(s.address, client.address)
                if (client.notes.isNotBlank()) InfoRow(s.notes, client.notes)
            }
        }

        Spacer(Modifier.height(14.dp))
        PrimaryButton(s.newInvoice, {
            Repo.prefs.edit().putString("bizana.prefill_client", clientId).apply()
            app.push(Screen.InvoiceEditor(null))
        }, Modifier.fillMaxWidth(), accent = b.blue, onAccent = b.onBlue, icon = Icons.Rounded.Add)

        Spacer(Modifier.height(16.dp))
        Text(s.clientInvoices, color = b.textDim, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        invd.forEach { inv ->
            val t = invoiceTotals(inv, items, payments)
            val eff = effectiveStatus(inv, t)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(14.dp)).background(b.surface)
                    .border(1.dp, b.hairline, RoundedCornerShape(14.dp))
                    .noRippleClickable { app.push(Screen.InvoiceDetail(inv.id)) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(inv.number, color = b.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(fmtDate(inv.date, lang), color = b.textDim, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(money(t.total), color = b.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    StatusBadge(s.statusLabel(eff), b.statusColor(eff))
                }
            }
        }

        if (clientPayments.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(s.paymentHistoryW, color = b.textDim, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            clientPayments.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(b.series[3]))
                    Spacer(Modifier.width(10.dp))
                    Text(fmtDate(p.date, lang) + " · " + s.methodLabel(p.method), color = b.textDim,
                        fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(money(p.amount), color = b.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    Sheet(edit, s.edit, onDismiss = { edit = false }) {
        Field(s.name, name, { name = it }, accent = b.blue)
        Field(s.phone, phone, { phone = it }, accent = b.blue)
        Field(s.email, email, { email = it }, accent = b.blue)
        Field(s.address, address, { address = it }, singleLine = false, accent = b.blue)
        Field(s.notes, notes, { notes = it }, singleLine = false, accent = b.blue)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(s.save, {
            Repo.clients.upsert(client.copy(name = name.trim(), phone = phone.trim(), email = email.trim(),
                address = address.trim(), notes = notes.trim(), updatedAt = nowIso()))
            app.showToast(s.savedOk); edit = false
        }, Modifier.fillMaxWidth(), accent = b.blue, onAccent = b.onBlue)
    }

    ConfirmDialog(confirmDel, s.confirmDelete, s.delete,
        onConfirm = { Repo.clients.softDelete(clientId); app.showToast(s.deletedOk); app.pop() },
        onDismiss = { confirmDel = false })
}

@Composable
private fun InfoRow(label: String, value: String) {
    val b = LocalB.current
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = b.muted, fontSize = 13.sp, modifier = Modifier.width(90.dp), fontWeight = FontWeight.Medium)
        Text(value, color = b.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}
