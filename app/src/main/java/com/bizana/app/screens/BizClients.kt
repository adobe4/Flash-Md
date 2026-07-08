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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Upload
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
import com.bizana.app.data.invoiceTotals
import com.bizana.app.data.money
import com.bizana.app.data.nowIso
import com.bizana.app.i18n.LocalStr
import com.bizana.app.ui.Card
import com.bizana.app.ui.EmptyState
import com.bizana.app.ui.Fab
import com.bizana.app.ui.Field
import com.bizana.app.ui.LocalB
import com.bizana.app.ui.PrimaryButton
import com.bizana.app.ui.Sheet
import com.bizana.app.ui.noRippleClickable

@Composable
fun ClientsScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val allClients by Repo.clients.flow.collectAsState()
    val invoices by Repo.invoices.flow.collectAsState()
    val items by Repo.invoiceItems.flow.collectAsState()
    val payments by Repo.payments.flow.collectAsState()
    val clients = allClients.filter { it.deletedAt == null }

    var query by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val filtered = clients.filter { query.isBlank() || it.name.contains(query, true) }
        .sortedBy { it.name.lowercase() }

    Box(Modifier.fillMaxWidth()) {
        ScreenColumn {
            ScreenTitle(s.clients, sub = s.business)

            Field("", query, { query = it }, placeholder = s.search, accent = b.blue)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolChip(s.exportJson, Icons.Rounded.Download) {
                    app.share(com.bizana.app.OutFile("bizana-clients.json", "application/json", text = Repo.clients.encode()))
                }
                ToolChip(s.importW, Icons.Rounded.Upload) {
                    app.importJson { raw ->
                        val n = try { Repo.clients.decodeAndMerge(raw) } catch (_: Exception) { -1 }
                        app.showToast(if (n >= 0) "$n ${s.itemsLoaded}" else s.importFailed)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                EmptyState(s.emptyList, s.addSomething, Icons.Rounded.People)
            } else {
                filtered.forEach { cl ->
                    val invd = invoices.filter { it.deletedAt == null && it.clientId == cl.id && it.status != "cancelled" }
                    val lifetime = invd.sumOf { invoiceTotals(it, items, payments).total }
                    val paid = invd.sumOf { invoiceTotals(it, items, payments).paid }
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.fillMaxWidth().noRippleClickable { app.push(Screen.ClientDetail(cl.id)) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).clip(CircleShape).background(b.blue.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center) {
                                    Text(cl.name.take(1).uppercase(), color = b.blue, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(cl.name, color = b.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (cl.phone.isNotBlank())
                                        Text(cl.phone, color = b.textDim, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MiniStat(s.lifetimeInvoiced, money(lifetime), b.blue, Modifier.weight(1f))
                                MiniStat(s.totalPaid, money(paid), b.series[3], Modifier.weight(1f))
                                MiniStat(s.balanceOwed, money(lifetime - paid), b.amber, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        Fab(onClick = { name = ""; phone = ""; email = ""; address = ""; notes = ""; sheet = true },
            accent = b.blue, onAccent = b.onBlue,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp))
    }

    Sheet(sheet, s.newClient, onDismiss = { sheet = false }) {
        Field(s.name, name, { name = it }, accent = b.blue)
        Field(s.phone, phone, { phone = it }, accent = b.blue)
        Field(s.email, email, { email = it }, accent = b.blue)
        Field(s.address, address, { address = it }, singleLine = false, accent = b.blue)
        Field(s.notes, notes, { notes = it }, singleLine = false, accent = b.blue)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(s.save, {
            if (name.isBlank()) { app.showToast(s.amountRequired); return@PrimaryButton }
            Repo.clients.upsert(Client(name = name.trim(), phone = phone.trim(), email = email.trim(),
                address = address.trim(), notes = notes.trim()))
            app.showToast(s.savedOk); sheet = false
        }, Modifier.fillMaxWidth(), accent = b.blue, onAccent = b.onBlue)
    }
}
