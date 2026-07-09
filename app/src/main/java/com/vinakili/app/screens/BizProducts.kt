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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
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
import com.vinakili.app.data.Product
import com.vinakili.app.data.Repo
import com.vinakili.app.data.money
import com.vinakili.app.data.nowIso
import com.vinakili.app.i18n.LocalStr
import com.vinakili.app.i18n.unitLabel
import com.vinakili.app.ui.AmountField
import com.vinakili.app.ui.Card
import com.vinakili.app.ui.Dropdown
import com.vinakili.app.ui.EmptyState
import com.vinakili.app.ui.Fab
import com.vinakili.app.ui.Field
import com.vinakili.app.ui.LocalB
import com.vinakili.app.ui.PrimaryButton
import com.vinakili.app.ui.SegPicker
import com.vinakili.app.ui.Sheet
import com.vinakili.app.ui.longPressable
import com.vinakili.app.ui.toAmount

@Composable
fun ProductsScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val all by Repo.products.flow.collectAsState()
    val products = all.filter { it.deletedAt == null }

    var query by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("piece") }
    var price by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("product") }

    val filtered = products.filter { query.isBlank() || it.name.contains(query, true) }.sortedBy { it.name.lowercase() }

    Box(Modifier.fillMaxWidth()) {
        ScreenColumn {
            ScreenTitle(s.products, sub = s.business)
            Field("", query, { query = it }, placeholder = s.search, accent = b.blue)
            Spacer(Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                EmptyState(s.emptyList, s.addSomething, Icons.Rounded.Inventory2)
            } else {
                filtered.forEach { p ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().longPressable(
                            onClick = { editing = p; name = p.name; desc = p.description; unit = p.unit
                                price = if (p.unitPrice == 0.0) "" else trimP(p.unitPrice); cat = p.category; sheet = true },
                            onLongClick = { Repo.products.softDelete(p.id); app.showToast(s.deletedOk) }),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, color = b.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text((if (p.category == "service") s.serviceW else s.productW) + " · " + s.unitLabel(p.unit),
                                    color = b.textDim, fontSize = 12.sp)
                            }
                            Text(money(p.unitPrice), color = b.blue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        Fab(onClick = { editing = null; name = ""; desc = ""; unit = "piece"; price = ""; cat = "product"; sheet = true },
            accent = b.blue, onAccent = b.onBlue,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 92.dp))
    }

    Sheet(sheet, if (editing == null) s.add else s.edit, onDismiss = { sheet = false }) {
        SegPicker(listOf("product" to s.productW, "service" to s.serviceW), cat, { cat = it }, b.blue, b.onBlue)
        Field(s.name, name, { name = it }, accent = b.blue)
        Field(s.description, desc, { desc = it }, singleLine = false, accent = b.blue)
        Dropdown(s.unit, listOf("piece" to s.piece, "hour" to s.hour, "kg" to s.kg,
            "month" to s.month, "service" to s.service), unit, { unit = it }, b.blue)
        AmountField(s.unitPrice, price, { price = it }, b.blue)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(s.save, {
            if (name.isBlank()) { app.showToast(s.amountRequired); return@PrimaryButton }
            val base = editing ?: Product()
            Repo.products.upsert(base.copy(name = name.trim(), description = desc.trim(), unit = unit,
                unitPrice = price.toAmount(), category = cat, updatedAt = nowIso()))
            app.showToast(s.savedOk); sheet = false
        }, Modifier.fillMaxWidth(), accent = b.blue, onAccent = b.onBlue)
    }
}

private fun trimP(v: Double) = if (v == Math.floor(v)) v.toInt().toString() else v.toString()
