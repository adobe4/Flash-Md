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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Payments
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
import com.bizana.app.data.PersonalItem
import com.bizana.app.data.Repo
import com.bizana.app.data.lastMonthKeys
import com.bizana.app.data.money
import com.bizana.app.data.moneyCompact
import com.bizana.app.data.monthShort
import com.bizana.app.data.thisYear
import com.bizana.app.data.thisYm
import com.bizana.app.data.ymOf
import com.bizana.app.i18n.LocalStr
import com.bizana.app.ui.AmountField
import com.bizana.app.ui.Bars
import com.bizana.app.ui.BarChart
import com.bizana.app.ui.Card
import com.bizana.app.ui.EmptyState
import com.bizana.app.ui.Fab
import com.bizana.app.ui.Field
import com.bizana.app.ui.LineChart
import com.bizana.app.ui.LocalB
import com.bizana.app.ui.PrimaryButton
import com.bizana.app.ui.SectionTitle
import com.bizana.app.ui.Sheet
import com.bizana.app.ui.longPressable
import com.bizana.app.ui.toAmount

@Composable
fun SpendScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val all by Repo.personalItems.flow.collectAsState()
    val spend = all.filter { it.deletedAt == null && it.type == "spend" }

    var sheet by remember { mutableStateOf(false) }
    var what by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    val monthTotal = spend.filter { ymOf(it.createdAt.take(10)) == thisYm() }.sumOf { it.amount }
    val yearTotal = spend.filter { it.createdAt.take(4) == thisYear() }.sumOf { it.amount }
    val allTotal = spend.sumOf { it.amount }

    val lang = Repo.lang.value ?: "en"
    val months = lastMonthKeys(6)
    val monthly = months.map { m -> Bars(monthShort(m, lang),
        listOf(spend.filter { ymOf(it.createdAt.take(10)) == m }.sumOf { it.amount }.toFloat())) }

    // last 6 years line
    val curYear = thisYear().toInt()
    val years = (curYear - 5..curYear).toList()
    val yearly = years.map { y -> spend.filter { it.createdAt.take(4) == y.toString() }.sumOf { it.amount }.toFloat() }

    Box(Modifier.fillMaxWidth()) {
        ScreenColumn {
            ScreenTitle(s.spend, sub = s.personal)

            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(s.thisMonth, color = b.textDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(money(monthTotal), color = b.amber, fontWeight = FontWeight.Black, fontSize = 30.sp)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat(s.thisYear, money(yearTotal), b.blue, Modifier.weight(1f))
                    MiniStat(s.allTime, money(allTotal), b.muted, Modifier.weight(1f))
                }
            }

            if (spend.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    SectionTitle(s.last6Months)
                    Spacer(Modifier.height(8.dp))
                    BarChart(monthly, listOf(b.amber))
                }
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    SectionTitle(s.yearlyTrend)
                    Spacer(Modifier.height(8.dp))
                    LineChart(yearly, years.map { it.toString() }, b.blue, area = true)
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionTitle(s.spend)
            if (spend.isEmpty()) {
                EmptyState(s.emptyList, s.addSomething, Icons.Rounded.Payments)
            } else {
                spend.sortedByDescending { it.createdAt }.forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(14.dp)).background(b.surface)
                            .border(1.dp, b.hairline, RoundedCornerShape(14.dp))
                            .longPressable(onClick = {}, onLongClick = { Repo.personalItems.softDelete(e.id); app.showToast(s.deletedOk) })
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(b.amber))
                        Spacer(Modifier.width(12.dp))
                        Text(e.name.ifBlank { "—" }, color = b.text, fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(money(e.amount), color = b.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
        Fab(onClick = { what = ""; amount = ""; sheet = true }, accent = b.amber, onAccent = b.onAmber,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp))
    }

    Sheet(sheet, s.add, onDismiss = { sheet = false }) {
        Field(s.what, what, { what = it }, accent = b.amber)
        AmountField(s.amount, amount, { amount = it }, b.amber)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(s.save, {
            if (what.isBlank() || amount.toAmount() <= 0) { app.showToast(s.amountRequired); return@PrimaryButton }
            Repo.personalItems.upsert(PersonalItem(type = "spend", name = what.trim(), amount = amount.toAmount()))
            app.showToast(s.savedOk); sheet = false
        }, Modifier.fillMaxWidth(), accent = b.amber, onAccent = b.onAmber)
    }
}
