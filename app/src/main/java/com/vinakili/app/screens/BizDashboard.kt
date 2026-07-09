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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinakili.app.AppState
import com.vinakili.app.Screen
import com.vinakili.app.data.Repo
import com.vinakili.app.data.effectiveStatus
import com.vinakili.app.data.invoiceTotals
import com.vinakili.app.data.lastMonthKeys
import com.vinakili.app.data.money
import com.vinakili.app.data.moneyCompact
import com.vinakili.app.data.thisYm
import com.vinakili.app.data.ymOf
import com.vinakili.app.i18n.LocalStr
import com.vinakili.app.i18n.statusLabel
import com.vinakili.app.ui.Card
import com.vinakili.app.ui.EmptyState
import com.vinakili.app.ui.LineChart
import com.vinakili.app.ui.LocalB
import com.vinakili.app.ui.SectionTitle
import com.vinakili.app.ui.StatTile
import com.vinakili.app.ui.StatusBadge
import com.vinakili.app.ui.noRippleClickable
import com.vinakili.app.ui.statusColor

@Composable
fun DashboardScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val invoices by Repo.invoices.flow.collectAsState()
    val items by Repo.invoiceItems.flow.collectAsState()
    val payments by Repo.payments.flow.collectAsState()
    val clients by Repo.clients.flow.collectAsState()

    val live = invoices.filter { it.deletedAt == null }
    val month = thisYm()

    val d = androidx.compose.runtime.remember(invoices, items, payments, clients) {
        var revenueMonth = 0.0; var paidMonth = 0.0
        var unpaidCount = 0; var unpaidAmt = 0.0; var overdueCount = 0
        live.forEach { inv ->
            val t = invoiceTotals(inv, items, payments)
            val eff = effectiveStatus(inv, t)
            if (inv.status != "cancelled" && ymOf(inv.date) == month) revenueMonth += t.total
            payments.filter { it.deletedAt == null && it.invoiceId == inv.id && ymOf(it.date) == month }
                .forEach { paidMonth += it.amount }
            if (eff == "sent" || eff == "overdue") { unpaidCount++; unpaidAmt += t.balance }
            if (eff == "overdue") overdueCount++
        }
        val days = (6 downTo 0).map { com.vinakili.app.data.addDays(com.vinakili.app.data.todayStr(), -it) }
        val rev7 = days.map { day -> payments.filter { it.deletedAt == null && it.date == day }.sumOf { it.amount }.toFloat() }
        val dayLabels = days.map { it.substring(8) }
        val topClients = clients.filter { it.deletedAt == null }.map { cl ->
            val rev = live.filter { it.clientId == cl.id && ymOf(it.date) == month && it.status != "cancelled" }
                .sumOf { invoiceTotals(it, items, payments).total }
            cl to rev
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(3)
        DashData(revenueMonth, paidMonth, unpaidCount, unpaidAmt, overdueCount, rev7, dayLabels, topClients)
    }
    val revenueMonth = d.revenueMonth; val paidMonth = d.paidMonth
    val unpaidCount = d.unpaidCount; val unpaidAmt = d.unpaidAmt; val overdueCount = d.overdueCount
    val rev7 = d.rev7; val dayLabels = d.dayLabels; val topClients = d.topClients

    ScreenColumn {
        ScreenTitle(s.dashboard, sub = s.business)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(s.revenueThisMonth, money(revenueMonth), b.blue, Modifier.weight(1f))
            StatTile(s.paidThisMonth, money(paidMonth), b.series[3], Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(s.unpaidInvoices, money(unpaidAmt), b.amber, Modifier.weight(1f), sub = "$unpaidCount ${s.items}")
            StatTile(s.overdueCount, overdueCount.toString(), b.danger, Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(s.newInvoice, Icons.Rounded.ReceiptLong, b.blue, b.onBlue, Modifier.weight(1f)) {
                app.push(Screen.InvoiceEditor(null))
            }
            QuickAction(s.newClient, Icons.Rounded.PersonAdd, b.surface2, b.text, Modifier.weight(1f)) {
                app.businessTab = 1
            }
            QuickAction(s.newExpense, Icons.Rounded.RemoveCircleOutline, b.surface2, b.text, Modifier.weight(1f)) {
                app.businessTab = 4
            }
        }

        if (live.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Card(Modifier.fillMaxWidth()) {
                SectionTitle(s.revenue7Days)
                Spacer(Modifier.height(10.dp))
                LineChart(rev7, dayLabels, b.blue, area = true)
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionTitle(s.recentInvoices)
        if (live.isEmpty()) {
            EmptyState(s.emptyList, s.addSomething, Icons.Rounded.ReceiptLong)
        } else {
            live.sortedByDescending { it.createdAt }.take(5).forEach { inv ->
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
                        Text(inv.clientName.ifBlank { "—" }, color = b.textDim, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(money(t.total), color = b.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        StatusBadge(s.statusLabel(eff), b.statusColor(eff))
                    }
                }
            }
        }

        if (topClients.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Card(Modifier.fillMaxWidth()) {
                SectionTitle(s.topClients)
                Spacer(Modifier.height(6.dp))
                topClients.forEachIndexed { i, (cl, rev) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(b.blue.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = b.blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(cl.name, color = b.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            modifier = Modifier.weight(1f))
                        Text(money(rev), color = b.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, bg: androidx.compose.ui.graphics.Color,
                        fg: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    val b = LocalB.current
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(bg)
            .border(1.dp, b.hairline, RoundedCornerShape(16.dp))
            .noRippleClickable(onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
    }
}

private data class DashData(
    val revenueMonth: Double,
    val paidMonth: Double,
    val unpaidCount: Int,
    val unpaidAmt: Double,
    val overdueCount: Int,
    val rev7: List<Float>,
    val dayLabels: List<String>,
    val topClients: List<Pair<com.vinakili.app.data.Client, Double>>,
)
