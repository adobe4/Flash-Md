package com.vinakili.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinakili.app.AppState
import com.vinakili.app.data.Repo
import com.vinakili.app.data.lastMonthKeys
import com.vinakili.app.data.monthShort
import com.vinakili.app.data.moneyCompact
import com.vinakili.app.data.ymOf
import com.vinakili.app.i18n.LocalStr
import com.vinakili.app.ui.Bars
import com.vinakili.app.ui.BarChart
import com.vinakili.app.ui.Card
import com.vinakili.app.ui.DonutChart
import com.vinakili.app.ui.EmptyState
import com.vinakili.app.ui.LocalB
import com.vinakili.app.ui.SectionTitle
import com.vinakili.app.ui.Slice

@Composable
fun StatsScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val lang = Repo.lang.value ?: "en"
    val all by Repo.personalItems.flow.collectAsState()
    val items = all.filter { it.deletedAt == null }

    val debtActive = items.filter { it.type == "debt" && !it.done }.sumOf { it.amount }
    val buyActive = items.filter { it.type == "buy" && !it.done }.sumOf { it.amount }
    val spendAll = items.filter { it.type == "spend" }.sumOf { it.amount }
    val hasData = items.isNotEmpty()

    val months = lastMonthKeys(6)
    val monthly = months.map { m ->
        val inMonth = items.filter { ymOf(it.createdAt.take(10)) == m }
        Bars(monthShort(m, lang), listOf(
            inMonth.filter { it.type == "debt" }.sumOf { it.amount }.toFloat(),
            inMonth.filter { it.type == "buy" }.sumOf { it.amount }.toFloat(),
            inMonth.filter { it.type == "spend" }.sumOf { it.amount }.toFloat(),
        ))
    }

    ScreenColumn {
        ScreenTitle(s.stats, sub = s.personal)
        if (!hasData) {
            EmptyState(s.noDataYet, s.addSomething, Icons.Rounded.BarChart)
        } else {
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                SectionTitle(s.monthlyOverview)
                Spacer(Modifier.height(10.dp))
                BarChart(monthly, listOf(b.amber, b.blue, b.series[2]))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Legend(s.debt, b.amber)
                    Legend(s.toBuy, b.blue)
                    Legend(s.spend, b.series[2])
                }
            }
            Spacer(Modifier.height(12.dp))
            val slices = buildList {
                if (debtActive > 0) add(Slice(s.debt, debtActive.toFloat(), b.amber))
                if (buyActive > 0) add(Slice(s.toBuy, buyActive.toFloat(), b.blue))
                if (spendAll > 0) add(Slice(s.spend, spendAll.toFloat(), b.series[2]))
            }
            if (slices.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    SectionTitle(s.breakdown)
                    Spacer(Modifier.height(10.dp))
                    DonutChart(slices, centerValue = moneyCompact((debtActive + buyActive + spendAll)))
                }
            }
        }
    }
}

@Composable
private fun Legend(label: String, color: androidx.compose.ui.graphics.Color) {
    val b = LocalB.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = b.textDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
