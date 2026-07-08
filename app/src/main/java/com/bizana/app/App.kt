package com.bizana.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizana.app.data.Repo
import com.bizana.app.i18n.LocalStr
import com.bizana.app.i18n.Str
import com.bizana.app.i18n.strFor
import com.bizana.app.screens.BalanceScreen
import com.bizana.app.screens.ClientDetailScreen
import com.bizana.app.screens.ClientsScreen
import com.bizana.app.screens.DashboardScreen
import com.bizana.app.screens.ExpensesScreen
import com.bizana.app.screens.GoalsScreen
import com.bizana.app.screens.InvoiceDetailScreen
import com.bizana.app.screens.InvoiceEditorScreen
import com.bizana.app.screens.InvoicesScreen
import com.bizana.app.screens.LanguagePicker
import com.bizana.app.screens.ListsScreen
import com.bizana.app.screens.ProductsScreen
import com.bizana.app.screens.ReportsScreen
import com.bizana.app.screens.SettingsScreen
import com.bizana.app.screens.SpendScreen
import com.bizana.app.screens.StatsScreen
import com.bizana.app.ui.BizanaTheme
import com.bizana.app.ui.BoxScopeToastHost
import com.bizana.app.ui.LocalB
import com.bizana.app.ui.accent
import com.bizana.app.ui.noRippleClickable
import kotlinx.coroutines.delay

data class TabDef(val label: String, val icon: ImageVector)

private data class ScreenKey(val business: Boolean, val tab: Int, val screen: Screen)

@Composable
fun BizanaApp(app: AppState) {
    val lang by Repo.lang.collectAsState()
    val theme by Repo.theme.collectAsState()
    val dark = theme != "light"
    val s = strFor(lang)

    BizanaTheme(dark = dark, business = app.business) {
        androidx.compose.runtime.CompositionLocalProvider(LocalStr provides s) {
            val b = LocalB.current
            Box(Modifier.fillMaxSize().background(b.bg)) {
                if (lang == null) {
                    LanguagePicker(onPick = { Repo.setLang(it) })
                } else {
                    MainShell(app, s)
                }

                // toast auto-dismiss
                LaunchedEffect(app.toast) {
                    if (app.toast != null) { delay(2200); app.toast = null }
                }
                BoxScopeToastHost(app.toast, Modifier.align(Alignment.TopCenter).statusBarsPadding())
            }
        }
    }
}

@Composable
private fun MainShell(app: AppState, s: Str) {
    val b = LocalB.current
    val personalTabs = listOf(
        TabDef(s.lists, Icons.Rounded.Checklist),
        TabDef(s.spend, Icons.Rounded.Payments),
        TabDef(s.balance, Icons.Rounded.AccountBalanceWallet),
        TabDef(s.goals, Icons.Rounded.EmojiEvents),
        TabDef(s.stats, Icons.Rounded.BarChart),
    )
    val businessTabs = listOf(
        TabDef(s.dashboard, Icons.Rounded.SpaceDashboard),
        TabDef(s.clients, Icons.Rounded.People),
        TabDef(s.invoices, Icons.Rounded.ReceiptLong),
        TabDef(s.products, Icons.Rounded.Inventory2),
        TabDef(s.expenses, Icons.Rounded.CreditCard),
        TabDef(s.reports, Icons.Rounded.Assessment),
    )
    val tabs = if (app.business) businessTabs else personalTabs
    val tabIndex = if (app.business) app.businessTab else app.personalTab

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header(app, s)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = ScreenKey(app.business, tabIndex, app.current),
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInHorizontally(tween(240)) { it / 12 })
                            .togetherWith(fadeOut(tween(140)))
                    },
                    label = "content",
                ) { key ->
                    when (val screen = key.screen) {
                        is Screen.Root -> RootContent(app, s)
                        is Screen.Settings -> SettingsScreen(app)
                        is Screen.ClientDetail -> ClientDetailScreen(app, screen.clientId)
                        is Screen.InvoiceEditor -> InvoiceEditorScreen(app, screen.invoiceId)
                        is Screen.InvoiceDetail -> InvoiceDetailScreen(app, screen.invoiceId)
                    }
                }
            }
            if (app.current is Screen.Root) {
                BottomNav(app, s, tabs, tabIndex)
            }
        }
    }
}

@Composable
private fun RootContent(app: AppState, s: Str) {
    if (app.business) {
        when (app.businessTab) {
            0 -> DashboardScreen(app)
            1 -> ClientsScreen(app)
            2 -> InvoicesScreen(app)
            3 -> ProductsScreen(app)
            4 -> ExpensesScreen(app)
            5 -> ReportsScreen(app)
        }
    } else {
        when (app.personalTab) {
            0 -> ListsScreen(app)
            1 -> SpendScreen(app)
            2 -> BalanceScreen(app)
            3 -> GoalsScreen(app)
            4 -> StatsScreen(app)
        }
    }
}

@Composable
private fun Header(app: AppState, s: Str) {
    val b = LocalB.current
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // logo mark
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                .background(b.accent(app.business)), contentAlignment = Alignment.Center) {
                Text("B", color = if (app.business) b.onBlue else b.onAmber,
                    fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
            Spacer(Modifier.width(9.dp))
            Text(s.appName, color = b.text, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
        // section switcher
        Row(
            Modifier.clip(CircleShape).background(b.surface2).border(1.dp, b.hairline, CircleShape)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SwitchPill(s.personal, !app.business, b.amber, b.onAmber) {
                if (app.business) { app.business = false; app.stack.clear() }
            }
            SwitchPill(s.business, app.business, b.blue, b.onBlue) {
                if (!app.business) { app.business = true; app.stack.clear() }
            }
        }
        // settings gear
        Box(Modifier.size(38.dp).clip(CircleShape).background(b.surface2)
            .border(1.dp, b.hairline, CircleShape)
            .noRippleClickable { if (app.current !is Screen.Settings) app.push(Screen.Settings) },
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Settings, s.settings, tint = b.textDim, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SwitchPill(label: String, selected: Boolean, accent: Color, onAccent: Color, onClick: () -> Unit) {
    val b = LocalB.current
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) accent else Color.Transparent)
            .noRippleClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) onAccent else b.textDim,
            fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
    }
}

@Composable
private fun BottomNav(app: AppState, s: Str, tabs: List<TabDef>, tabIndex: Int) {
    val b = LocalB.current
    val accent = b.accent(app.business)
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).navigationBarsPadding().padding(bottom = 8.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(b.surface)
                .border(1.dp, b.hairline, RoundedCornerShape(22.dp))
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, tab ->
                val sel = i == tabIndex
                Column(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .noRippleClickable {
                            if (app.business) app.businessTab = i else app.personalTab = i
                        }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp))
                            .background(if (sel) accent.copy(alpha = 0.16f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tab.icon, tab.label, tint = if (sel) accent else b.muted,
                            modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.size(3.dp))
                    Text(tab.label, color = if (sel) b.text else b.muted,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, fontSize = 9.5.sp,
                        maxLines = 1)
                }
            }
        }
    }
}
