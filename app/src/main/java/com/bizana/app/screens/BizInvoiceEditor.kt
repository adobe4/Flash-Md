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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.bizana.app.data.Invoice
import com.bizana.app.data.InvoiceItem
import com.bizana.app.data.Repo
import com.bizana.app.data.StatusChange
import com.bizana.app.data.money
import com.bizana.app.data.nowIso
import com.bizana.app.data.todayStr
import com.bizana.app.data.uuid
import com.bizana.app.i18n.LocalStr
import com.bizana.app.ui.AmountField
import com.bizana.app.ui.Card
import com.bizana.app.ui.Field
import com.bizana.app.ui.GhostButton
import com.bizana.app.ui.LocalB
import com.bizana.app.ui.PrimaryButton
import com.bizana.app.ui.SectionTitle
import com.bizana.app.ui.SegPicker
import com.bizana.app.ui.Sheet
import com.bizana.app.ui.noRippleClickable
import com.bizana.app.ui.toAmount

private class ItemDraft(
    val id: String,
    var description: String,
    var qty: String,
    var unitPrice: String,
)

@Composable
fun InvoiceEditorScreen(app: AppState, invoiceId: String?) {
    val b = LocalB.current
    val s = LocalStr.current
    val existing = invoiceId?.let { id -> Repo.invoices.flow.value.firstOrNull { it.id == id } }
    val clients by Repo.clients.flow.collectAsState()
    val products by Repo.products.flow.collectAsState()
    val liveClients = clients.filter { it.deletedAt == null }
    val liveProducts = products.filter { it.deletedAt == null }
    val biz by Repo.bizSettings.collectAsState()

    val prefillClient = remember {
        val id = Repo.prefs.getString("bizana.prefill_client", null)
        Repo.prefs.edit().remove("bizana.prefill_client").apply()
        id
    }

    var clientId by remember { mutableStateOf(existing?.clientId ?: (prefillClient ?: "")) }
    var clientName by remember {
        mutableStateOf(existing?.clientName
            ?: liveClients.firstOrNull { it.id == prefillClient }?.name ?: "")
    }
    var date by remember { mutableStateOf(existing?.date ?: todayStr()) }
    var dueDate by remember { mutableStateOf(existing?.dueDate ?: todayStr()) }
    var discountType by remember { mutableStateOf(existing?.discountType ?: "pct") }
    var discountValue by remember { mutableStateOf(existing?.discountValue?.takeIf { it > 0 }?.toString() ?: "") }
    var vatPct by remember { mutableStateOf((existing?.vatPct ?: biz.defaultVat).takeIf { it > 0 }?.toString() ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: biz.paymentNotes) }

    val drafts = remember {
        mutableStateListOf<ItemDraft>().apply {
            if (existing != null) {
                Repo.invoiceItems.flow.value.filter { it.deletedAt == null && it.invoiceId == existing.id }
                    .forEach { add(ItemDraft(it.id, it.description, trimNum(it.qty), trimNum(it.unitPrice))) }
            }
            if (isEmpty()) add(ItemDraft(uuid(), "", "1", ""))
        }
    }
    var refresh by remember { mutableStateOf(0) }
    var clientSheet by remember { mutableStateOf(false) }
    var catalogSheet by remember { mutableStateOf<Int?>(null) }

    fun subtotal() = drafts.sumOf { it.qty.toAmount() * it.unitPrice.toAmount() }
    val sub = run { refresh; subtotal() }
    val disc = if (discountType == "pct") sub * (discountValue.toAmount() / 100.0) else discountValue.toAmount()
    val afterDisc = (sub - disc).coerceAtLeast(0.0)
    val vat = afterDisc * (vatPct.toAmount() / 100.0)
    val grand = afterDisc + vat

    fun persist(status: String) {
        val id = existing?.id ?: uuid()
        val number = existing?.number ?: Repo.nextInvoiceNumber()
        val history = existing?.statusHistory ?: emptyList()
        val newHistory = if (existing == null || existing.status != status)
            history + StatusChange(status) else history
        Repo.invoices.upsert(
            (existing ?: Invoice(id = id)).copy(
                id = id, number = number, clientId = clientId, clientName = clientName.trim(),
                date = date, dueDate = dueDate, discountType = discountType,
                discountValue = discountValue.toAmount(), vatPct = vatPct.toAmount(),
                notes = notes.trim(), status = status, statusHistory = newHistory, updatedAt = nowIso(),
            )
        )
        // reconcile line items: soft-delete removed, upsert current
        val keepIds = drafts.map { it.id }.toSet()
        Repo.invoiceItems.flow.value.filter { it.invoiceId == id && it.deletedAt == null && it.id !in keepIds }
            .forEach { Repo.invoiceItems.softDelete(it.id) }
        drafts.filter { it.description.isNotBlank() || it.unitPrice.toAmount() > 0 }.forEach { d ->
            val prev = Repo.invoiceItems.flow.value.firstOrNull { it.id == d.id }
            Repo.invoiceItems.upsert((prev ?: InvoiceItem(id = d.id, invoiceId = id)).copy(
                invoiceId = id, description = d.description.trim(),
                qty = d.qty.toAmount().coerceAtLeast(1.0), unitPrice = d.unitPrice.toAmount(), updatedAt = nowIso()))
        }
        app.showToast(s.savedOk)
        app.pop()
    }

    OverlayScreen(if (existing == null) s.newInvoice else s.editInvoice, onBack = { app.pop() }) {
        // client
        SectionTitle(s.client)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(b.surface2)
                .border(1.dp, b.hairline, RoundedCornerShape(13.dp))
                .noRippleClickable { clientSheet = true }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(clientName.ifBlank { s.selectClient }, color = if (clientName.isBlank()) b.muted else b.text,
                fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.Add, null, tint = b.blue, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(s.invoiceDate, date, { date = it }, Modifier.weight(1f), placeholder = "yyyy-mm-dd", accent = b.blue)
            Field(s.dueDate, dueDate, { dueDate = it }, Modifier.weight(1f), placeholder = "yyyy-mm-dd", accent = b.blue)
        }

        Spacer(Modifier.height(8.dp))
        SectionTitle(s.lineItems)
        drafts.forEachIndexed { i, d ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = b.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.width(18.dp))
                    Field("", d.description, { d.description = it; refresh++ }, Modifier.weight(1f),
                        placeholder = s.description, accent = b.blue)
                    if (drafts.size > 1) {
                        Spacer(Modifier.width(4.dp))
                        Box(Modifier.size(28.dp).clip(CircleShape).background(b.surface2)
                            .noRippleClickable { drafts.removeAt(i); refresh++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, s.delete, tint = b.danger, modifier = Modifier.size(15.dp))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AmountField(s.qty, d.qty, { d.qty = it; refresh++ }, b.blue, Modifier.weight(0.8f))
                    AmountField(s.unitPrice, d.unitPrice, { d.unitPrice = it; refresh++ }, b.blue, Modifier.weight(1.4f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(s.subtotal, color = b.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(money(d.qty.toAmount() * d.unitPrice.toAmount()), color = b.text,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(s.addLineItem, { drafts.add(ItemDraft(uuid(), "", "1", "")); refresh++ },
                Modifier.weight(1f), icon = Icons.Rounded.Add)
            if (liveProducts.isNotEmpty())
                GhostButton(s.fromCatalog, { catalogSheet = drafts.size }, Modifier.weight(1f), icon = Icons.Rounded.Inventory2)
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            TotalRow(s.subtotal, money(sub))
            Spacer(Modifier.height(8.dp))
            Text(s.discount, color = b.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1.4f)) {
                    AmountField("", discountValue, { discountValue = it }, b.blue, placeholder = "0")
                }
                Box(Modifier.weight(1f)) {
                    SegPicker(listOf("pct" to "%", "amt" to "TZS"), discountType, { discountType = it }, b.blue, b.onBlue)
                }
            }
            AmountField(s.vat + " %", vatPct, { vatPct = it }, b.blue, placeholder = "0")
            Spacer(Modifier.height(4.dp))
            if (disc > 0) TotalRow(s.discount, "- " + money(disc))
            if (vat > 0) TotalRow(s.vat, money(vat))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(s.grandTotal, color = b.text, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(money(grand), color = b.blue, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
        }

        Spacer(Modifier.height(10.dp))
        Field(s.paymentNotes, notes, { notes = it }, singleLine = false, accent = b.blue)

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(s.saveDraft, { persist("draft") }, Modifier.weight(1f))
            PrimaryButton(s.markSent, { persist("sent") }, Modifier.weight(1f), accent = b.blue, onAccent = b.onBlue)
        }
    }

    // client picker
    Sheet(clientSheet, s.selectClient, onDismiss = { clientSheet = false }) {
        var newName by remember { mutableStateOf("") }
        Field(s.orTypeNew, newName, { newName = it }, accent = b.blue)
        if (newName.isNotBlank()) {
            PrimaryButton(s.add, {
                clientName = newName.trim(); clientId = ""; clientSheet = false
            }, Modifier.fillMaxWidth(), accent = b.blue, onAccent = b.onBlue)
        }
        Spacer(Modifier.height(10.dp))
        liveClients.forEach { cl ->
            Row(Modifier.fillMaxWidth().noRippleClickable {
                clientId = cl.id; clientName = cl.name; clientSheet = false
            }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(b.blue.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center) {
                    Text(cl.name.take(1).uppercase(), color = b.blue, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text(cl.name, color = b.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }

    // catalog picker
    Sheet(catalogSheet != null, s.fromCatalog, onDismiss = { catalogSheet = null }) {
        liveProducts.forEach { p ->
            Row(Modifier.fillMaxWidth().noRippleClickable {
                drafts.add(ItemDraft(uuid(), p.name, "1", trimNum(p.unitPrice))); refresh++; catalogSheet = null
            }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = b.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (p.description.isNotBlank())
                        Text(p.description, color = b.textDim, fontSize = 12.sp, maxLines = 1)
                }
                Text(money(p.unitPrice), color = b.blue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    val b = LocalB.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = b.textDim, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(value, color = b.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun trimNum(v: Double) = if (v == Math.floor(v)) v.toInt().toString() else v.toString()
