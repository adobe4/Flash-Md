package com.bizana.app.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
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
import com.bizana.app.data.Expense
import com.bizana.app.data.Repo
import com.bizana.app.data.addDays
import com.bizana.app.data.fmtDate
import com.bizana.app.data.lastMonthKeys
import com.bizana.app.data.money
import com.bizana.app.data.monthShort
import com.bizana.app.data.nowIso
import com.bizana.app.data.thisYear
import com.bizana.app.data.thisYm
import com.bizana.app.data.todayStr
import com.bizana.app.data.ymOf
import com.bizana.app.i18n.LocalStr
import com.bizana.app.i18n.expCatLabel
import com.bizana.app.ui.AmountField
import com.bizana.app.ui.Bars
import com.bizana.app.ui.BarChart
import com.bizana.app.ui.Card
import com.bizana.app.ui.Dropdown
import com.bizana.app.ui.DonutChart
import com.bizana.app.ui.EmptyState
import com.bizana.app.ui.Fab
import com.bizana.app.ui.Field
import com.bizana.app.ui.LocalB
import com.bizana.app.ui.Pill
import com.bizana.app.ui.PrimaryButton
import com.bizana.app.ui.SectionTitle
import com.bizana.app.ui.Sheet
import com.bizana.app.ui.Slice
import com.bizana.app.ui.longPressable
import com.bizana.app.ui.toAmount

private val EXP_CATS = listOf("stock", "transport", "rent", "salaries", "marketing", "utilities", "other")

@Composable
fun ExpensesScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val lang = Repo.lang.value ?: "en"
    val all by Repo.expenses.flow.collectAsState()
    val expenses = all.filter { it.deletedAt == null }

    var range by remember { mutableStateOf("month") }
    var catFilter by remember { mutableStateOf("all") }
    var sheet by remember { mutableStateOf(false) }
    var what by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("stock") }
    var date by remember { mutableStateOf(todayStr()) }
    var notes by remember { mutableStateOf("") }

    val weekAgo = addDays(todayStr(), -7)
    fun inRange(e: Expense) = when (range) {
        "week" -> e.date >= weekAgo
        "month" -> ymOf(e.date) == thisYm()
        "year" -> e.date.take(4) == thisYear()
        else -> true
    }
    val filtered = expenses.filter { inRange(it) && (catFilter == "all" || it.category == catFilter) }
        .sortedByDescending { it.date }

    val monthTotal = expenses.filter { ymOf(it.date) == thisYm() }.sumOf { it.amount }
    val yearTotal = expenses.filter { it.date.take(4) == thisYear() }.sumOf { it.amount }

    val months = lastMonthKeys(6)
    val monthly = months.map { m -> Bars(monthShort(m, lang),
        listOf(expenses.filter { ymOf(it.date) == m }.sumOf { it.amount }.toFloat())) }
    val catSlices = EXP_CATS.mapNotNull { c ->
        val sum = expenses.filter { it.category == c }.sumOf { it.amount }
        if (sum > 0) Slice(s.expCatLabel(c), sum.toFloat(), b.palette[EXP_CATS.indexOf(c) % b.palette.size]) else null
    }

    Box(Modifier.fillMaxWidth()) {
        ScreenColumn {
            ScreenTitle(s.expenses, sub = s.business)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(s.thisMonth, money(monthTotal), b.amber, Modifier.weight(1f))
                MiniStat(s.thisYear, money(yearTotal), b.blue, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(s.thisWeek, range == "week", b.blue, { range = "week" }, onAccent = b.onBlue)
                Pill(s.thisMonth, range == "month", b.blue, { range = "month" }, onAccent = b.onBlue)
                Pill(s.thisYear, range == "year", b.blue, { range = "year" }, onAccent = b.onBlue)
                Pill(s.allTime, range == "all", b.blue, { range = "all" }, onAccent = b.onBlue)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(s.all, catFilter == "all", b.amber, { catFilter = "all" }, onAccent = b.onAmber)
                EXP_CATS.forEach { c ->
                    Pill(s.expCatLabel(c), catFilter == c, b.amber, { catFilter = c }, onAccent = b.onAmber)
                }
            }

            if (expenses.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    SectionTitle(s.last6Months)
                    Spacer(Modifier.height(8.dp))
                    BarChart(monthly, listOf(b.amber))
                }
                if (catSlices.size >= 2) {
                    Spacer(Modifier.height(12.dp))
                    Card(Modifier.fillMaxWidth()) {
                        SectionTitle(s.byCategory)
                        Spacer(Modifier.height(8.dp))
                        DonutChart(catSlices)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                EmptyState(s.emptyList, s.addSomething, Icons.Rounded.CreditCard)
            } else {
                filtered.forEach { e ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(14.dp)).background(b.surface)
                        .border(1.dp, b.hairline, RoundedCornerShape(14.dp))
                        .longPressable(onClick = {}, onLongClick = { Repo.expenses.softDelete(e.id); app.showToast(s.deletedOk) })
                        .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape)
                            .background(b.palette[EXP_CATS.indexOf(e.category).coerceAtLeast(0) % b.palette.size]))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.what.ifBlank { "—" }, color = b.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("${s.expCatLabel(e.category)} · ${fmtDate(e.date, lang)}", color = b.textDim, fontSize = 11.5.sp)
                        }
                        Text(money(e.amount), color = b.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
        Fab(onClick = { what = ""; amount = ""; cat = "stock"; date = todayStr(); notes = ""; sheet = true },
            accent = b.blue, onAccent = b.onBlue,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp))
    }

    Sheet(sheet, s.newExpense, onDismiss = { sheet = false }) {
        Field(s.what, what, { what = it }, accent = b.blue)
        AmountField(s.amount, amount, { amount = it }, b.blue)
        Dropdown(s.category, EXP_CATS.map { it to s.expCatLabel(it) }, cat, { cat = it }, b.blue)
        Field(s.date, date, { date = it }, placeholder = "yyyy-mm-dd", accent = b.blue)
        Field(s.notes, notes, { notes = it }, singleLine = false, accent = b.blue)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(s.save, {
            if (what.isBlank() || amount.toAmount() <= 0) { app.showToast(s.amountRequired); return@PrimaryButton }
            Repo.expenses.upsert(Expense(what = what.trim(), category = cat, amount = amount.toAmount(),
                date = date.ifBlank { todayStr() }, notes = notes.trim()))
            app.showToast(s.savedOk); sheet = false
        }, Modifier.fillMaxWidth(), accent = b.blue, onAccent = b.onBlue)
    }
}
