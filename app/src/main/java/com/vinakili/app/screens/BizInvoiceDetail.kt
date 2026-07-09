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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PictureAsPdf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinakili.app.AppState
import com.vinakili.app.OutFile
import com.vinakili.app.Screen
import com.vinakili.app.data.Payment
import com.vinakili.app.data.Repo
import com.vinakili.app.data.effectiveStatus
import com.vinakili.app.data.fmtDate
import com.vinakili.app.data.invoiceTotals
import com.vinakili.app.data.money
import com.vinakili.app.data.nowIso
import com.vinakili.app.data.todayStr
import com.vinakili.app.i18n.LocalStr
import com.vinakili.app.i18n.methodLabel
import com.vinakili.app.i18n.statusLabel
import com.vinakili.app.platform.InvoicePdf
import com.vinakili.app.ui.Card
import com.vinakili.app.ui.AmountField
import com.vinakili.app.ui.Field
import com.vinakili.app.ui.GhostButton
import com.vinakili.app.ui.LocalB
import com.vinakili.app.ui.PrimaryButton
import com.vinakili.app.ui.SectionTitle
import com.vinakili.app.ui.SegPicker
import com.vinakili.app.ui.Sheet
import com.vinakili.app.ui.StatusBadge
import com.vinakili.app.ui.noRippleClickable
import com.vinakili.app.ui.statusColor
import com.vinakili.app.ui.toAmount

@Composable
fun InvoiceDetailScreen(app: AppState, invoiceId: String) {
    val b = LocalB.current
    val s = LocalStr.current
    val lang = Repo.lang.value ?: "en"
    val ctx = LocalContext.current
    val invoices by Repo.invoices.flow.collectAsState()
    val allItems by Repo.invoiceItems.flow.collectAsState()
    val allPayments by Repo.payments.flow.collectAsState()
    val clients by Repo.clients.flow.collectAsState()
    val biz by Repo.bizSettings.collectAsState()

    val inv = invoices.firstOrNull { it.id == invoiceId }
    if (inv == null) { app.pop(); return }
    val items = allItems.filter { it.deletedAt == null && it.invoiceId == invoiceId }
    val payments = allPayments.filter { it.deletedAt == null && it.invoiceId == invoiceId }.sortedByDescending { it.date }
    val client = clients.firstOrNull { it.id == inv.clientId }
    val t = invoiceTotals(inv, allItems, allPayments)
    val eff = effectiveStatus(inv, t)

    var paySheet by remember { mutableStateOf(false) }
    var payAmount by remember { mutableStateOf("") }
    var payDate by remember { mutableStateOf(todayStr()) }
    var payMethod by remember { mutableStateOf("cash") }

    fun exportPdf() {
        val bytes = InvoicePdf.build(inv, client, allItems, t, allPayments, biz, s.statusLabel(eff))
        app.share(OutFile("${inv.number}.pdf", "application/pdf", bytes = bytes))
    }

    OverlayScreen(inv.number, onBack = { app.pop() }, trailing = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconAction(Icons.Rounded.ContentCopy, b.textDim, s.duplicate) {
                duplicateInvoice(invoiceId); app.showToast(s.savedOk); app.pop()
            }
        }
    }) {
        // document preview
        Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(biz.name.ifBlank { "Vinakili" }, color = b.text, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    if (biz.address.isNotBlank())
                        Text(biz.address, color = b.textDim, fontSize = 11.sp)
                }
                StatusBadge(s.statusLabel(eff), b.statusColor(eff))
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(s.client, color = b.muted, fontSize = 11.sp)
                    Text(inv.clientName.ifBlank { "—" }, color = b.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    client?.phone?.takeIf { it.isNotBlank() }?.let { Text(it, color = b.textDim, fontSize = 11.sp) }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(s.invoiceDate, color = b.muted, fontSize = 11.sp)
                    Text(fmtDate(inv.date, lang), color = b.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(s.dueDate, color = b.muted, fontSize = 11.sp)
                    Text(fmtDate(inv.dueDate, lang), color = b.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(b.hairline))
            Spacer(Modifier.height(10.dp))
            items.forEach { it2 ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(it2.description.ifBlank { "—" }, color = b.text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("${trimN(it2.qty)} × ${money(it2.unitPrice)}", color = b.textDim, fontSize = 11.sp)
                    }
                    Text(money(it2.subtotal), color = b.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(b.hairline))
            Spacer(Modifier.height(10.dp))
            TotalRow2(s.subtotal, money(t.subtotal), b)
            if (t.discount > 0) TotalRow2(s.discount, "- " + money(t.discount), b)
            if (t.vat > 0) TotalRow2(s.vat, money(t.vat), b)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(s.grandTotal, color = b.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(money(t.total), color = b.blue, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            if (t.paid > 0) {
                Spacer(Modifier.height(6.dp))
                TotalRow2(s.totalPaid, "- " + money(t.paid), b)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(s.balanceRemaining, color = b.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(money(t.balance), color = if (t.balance > 0) b.amber else b.series[3],
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            if (inv.notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(s.paymentNotes, color = b.muted, fontSize = 11.sp)
                Text(inv.notes, color = b.textDim, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (eff != "cancelled" && t.balance > 0)
                PrimaryButton(s.recordPayment, { payAmount = trimN(t.balance); paySheet = true },
                    Modifier.weight(1f), accent = b.blue, onAccent = b.onBlue)
            GhostButton(s.pdf, { exportPdf() }, Modifier.weight(1f), icon = Icons.Rounded.PictureAsPdf)
        }

        if (payments.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionTitle(s.paymentHistoryW)
            payments.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(b.series[3]))
                    Spacer(Modifier.width(10.dp))
                    Text("${fmtDate(p.date, lang)} · ${s.methodLabel(p.method)}", color = b.textDim,
                        fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(money(p.amount), color = b.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (inv.statusHistory.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionTitle(s.statusHistory)
            inv.statusHistory.reversed().forEach { h ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(b.statusColor(h.status)))
                    Spacer(Modifier.width(10.dp))
                    Text(s.statusLabel(h.status), color = b.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    Text(fmtDate(h.at.take(10), lang), color = b.muted, fontSize = 12.sp)
                }
            }
        }
    }

    Sheet(paySheet, s.recordPayment, onDismiss = { paySheet = false }) {
        AmountField(s.amount, payAmount, { payAmount = it }, b.blue)
        Field(s.date, payDate, { payDate = it }, placeholder = "yyyy-mm-dd", accent = b.blue)
        Text(s.method, color = b.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp))
        SegPicker(listOf("cash" to s.cash, "mpesa" to s.mpesa, "bank" to s.bank, "other" to s.otherW),
            payMethod, { payMethod = it }, b.blue, b.onBlue)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(s.save, {
            val amt = payAmount.toAmount()
            if (amt <= 0) { app.showToast(s.amountRequired); return@PrimaryButton }
            Repo.payments.upsert(Payment(invoiceId = invoiceId, amount = amt, date = payDate.ifBlank { todayStr() }, method = payMethod))
            // auto-complete when fully paid
            val newT = invoiceTotals(inv, allItems, Repo.payments.flow.value)
            if (newT.balance <= 0 && inv.status != "paid") {
                Repo.invoices.upsert(inv.copy(status = "paid",
                    statusHistory = inv.statusHistory + com.vinakili.app.data.StatusChange("paid"), updatedAt = nowIso()))
            }
            app.showToast(s.savedOk); paySheet = false
        }, Modifier.fillMaxWidth(), accent = b.blue, onAccent = b.onBlue)
    }
}

@Composable
private fun TotalRow2(label: String, value: String, b: com.vinakili.app.ui.BColors) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = b.textDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(value, color = b.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun trimN(v: Double) = if (v == Math.floor(v)) v.toInt().toString() else v.toString()
