package com.vinakili.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
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
import com.vinakili.app.data.Repo
import com.vinakili.app.data.daysBetween
import com.vinakili.app.data.effectiveStatus
import com.vinakili.app.data.invoiceTotals
import com.vinakili.app.data.lastMonthKeys
import com.vinakili.app.data.money
import com.vinakili.app.data.moneyCompact
import com.vinakili.app.data.monthShort
import com.vinakili.app.data.thisYear
import com.vinakili.app.data.thisYm
import com.vinakili.app.data.todayStr
import com.vinakili.app.data.ymOf
import com.vinakili.app.i18n.LocalStr
import com.vinakili.app.ui.Card
import com.vinakili.app.ui.DonutChart
import com.vinakili.app.ui.EmptyState
import com.vinakili.app.ui.LineChart
import com.vinakili.app.ui.LocalB
import com.vinakili.app.ui.Pill
import com.vinakili.app.ui.SectionTitle
import com.vinakili.app.ui.Slice
import com.vinakili.app.ui.StatTile
import com.vinakili.app.ui.statusColor

@Composable
fun ReportsScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val lang = Repo.lang.value ?: "en"
    val invoices by Repo.invoices.flow.collectAsState()
    val items by Repo.invoiceItems.flow.collectAsState()
    val payments by Repo.payments.flow.collectAsState()
    val expenses by Repo.expenses.flow.collectAsState()
    val clients by Repo.clients.flow.collectAsState()

    var range by remember { mutableStateOf("month") }
    val live = invoices.filter { it.deletedAt == null }
    val liveExp = expenses.filter { it.deletedAt == null }

    fun invInRange(date: String) = when (range) {
        "month" -> ymOf(date) == thisYm()
        "3m" -> date >= com.vinakili.app.data.addDays(todayStr(), -92)
        "year" -> date.take(4) == thisYear()
        else -> true
    }

    // range-dependent totals (cheap: one pass)
    val revenue = live.filter { it.status != "cancelled" && invInRange(it.date) }
        .sumOf { invoiceTotals(it, items, payments).total }
    val expTotal = liveExp.filter { invInRange(it.date) }.sumOf { it.amount }
    val profit = revenue - expTotal
    val hasData = live.isNotEmpty() || liveExp.isNotEmpty()

    // heavy, range-independent aggregates — computed once per data change
    val agg = remember(invoices, items, payments, expenses, clients, lang) {
        val months = lastMonthKeys(12)
        val revSeries = months.map { m ->
            live.filter { it.status != "cancelled" && ymOf(it.date) == m }
                .sumOf { invoiceTotals(it, items, payments).total }.toFloat()
        }
        val expSeries = months.map { m -> liveExp.filter { ymOf(it.date) == m }.sumOf { it.amount }.toFloat() }
        val monthLabels = months.filterIndexed { i, _ -> i % 2 == 0 }.map { monthShort(it, lang) }
        var paidC = 0; var unpaidC = 0; var overdueC = 0; var draftC = 0
        live.forEach {
            when (effectiveStatus(it, invoiceTotals(it, items, payments))) {
                "paid" -> paidC++
                "overdue" -> overdueC++
                "draft" -> draftC++
                "sent" -> unpaidC++
                else -> {}
            }
        }
        val topClients = clients.filter { it.deletedAt == null }.map { cl ->
            cl to live.filter { it.clientId == cl.id && it.status != "cancelled" }
                .sumOf { invoiceTotals(it, items, payments).total }
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(5)
        val overdue = live.filter { effectiveStatus(it, invoiceTotals(it, items, payments)) == "overdue" }
            .sortedBy { it.dueDate }
        val freq = HashMap<String, Int>()
        items.filter { it.deletedAt == null }.forEach { it2 ->
            val key = it2.description.trim().lowercase()
            if (key.isNotBlank()) freq[key] = (freq[key] ?: 0) + 1
        }
        val bestSelling = freq.entries.sortedByDescending { it.value }.map { it.key to it.value }.take(5)
        ReportAgg(revSeries, expSeries, monthLabels, intArrayOf(paidC, unpaidC, overdueC, draftC),
            topClients, overdue, bestSelling)
    }
    val revSeries = agg.revSeries; val expSeries = agg.expSeries; val monthLabels = agg.monthLabels
    val paidC = agg.counts[0]; val unpaidC = agg.counts[1]; val overdueC = agg.counts[2]; val draftC = agg.counts[3]
    val topClients = agg.topClients; val overdue = agg.overdue; val bestSelling = agg.bestSelling
    val statusSlices = buildList {
        if (paidC > 0) add(Slice(s.paid, paidC.toFloat(), b.statusColor("paid")))
        if (unpaidC > 0) add(Slice(s.unpaid, unpaidC.toFloat(), b.statusColor("sent")))
        if (overdueC > 0) add(Slice(s.overdue, overdueC.toFloat(), b.statusColor("overdue")))
        if (draftC > 0) add(Slice(s.draft, draftC.toFloat(), b.statusColor("draft")))
    }

    ScreenColumn {
        ScreenTitle(s.reports, sub = s.business)
        if (!hasData) {
            EmptyState(s.noDataYet, s.addSomething, Icons.Rounded.Assessment)
            return@ScreenColumn
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(s.thisMonth, range == "month", b.blue, { range = "month" }, onAccent = b.onBlue)
            Pill(s.last3Months, range == "3m", b.blue, { range = "3m" }, onAccent = b.onBlue)
            Pill(s.thisYear, range == "year", b.blue, { range = "year" }, onAccent = b.onBlue)
            Pill(s.allTime, range == "all", b.blue, { range = "all" }, onAccent = b.onBlue)
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            SectionTitle(s.profitLoss)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(s.revenue, money(revenue), b.blue, Modifier.weight(1f))
                MiniStat(s.expenses, money(expTotal), b.amber, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(s.netProfit, color = b.text, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(money(profit), color = if (profit >= 0) b.series[3] else b.danger,
                    fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            SectionTitle(s.last12Months)
            Spacer(Modifier.height(10.dp))
            LineChart(revSeries, monthLabels, b.blue, area = true, secondValues = expSeries, secondColor = b.amber)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot(s.revenue, b.blue)
                LegendDot(s.expenses, b.amber)
            }
        }

        if (statusSlices.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                SectionTitle(s.invoiceStatusW)
                Spacer(Modifier.height(8.dp))
                DonutChart(statusSlices, centerValue = live.size.toString(), centerLabel = s.invoices)
            }
        }

        if (topClients.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                SectionTitle(s.topClients)
                Spacer(Modifier.height(6.dp))
                topClients.forEachIndexed { i, (cl, rev) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", color = b.blue, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(22.dp))
                        Text(cl.name, color = b.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(money(rev), color = b.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        if (overdue.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                SectionTitle(s.overdueInvoices)
                Spacer(Modifier.height(6.dp))
                overdue.forEach { inv ->
                    val t = invoiceTotals(inv, items, payments)
                    val d = daysBetween(inv.dueDate, todayStr())
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(inv.number + " · " + inv.clientName, color = b.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("$d ${s.daysOverdue}", color = b.danger, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(money(t.balance), color = b.danger, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        if (bestSelling.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                SectionTitle(s.bestSelling)
                Spacer(Modifier.height(6.dp))
                bestSelling.forEach { (nameKey, count) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(b.blue))
                        Spacer(Modifier.width(10.dp))
                        Text(nameKey.replaceFirstChar { it.uppercase() }, color = b.text,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("$count ${s.timesInvoiced}", color = b.textDim, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    val b = LocalB.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = b.textDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private data class ReportAgg(
    val revSeries: List<Float>,
    val expSeries: List<Float>,
    val monthLabels: List<String>,
    val counts: IntArray,
    val topClients: List<Pair<com.vinakili.app.data.Client, Double>>,
    val overdue: List<com.vinakili.app.data.Invoice>,
    val bestSelling: List<Pair<String, Int>>,
)
