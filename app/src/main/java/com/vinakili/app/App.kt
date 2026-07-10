package com.vinakili.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinakili.app.data.Repo
import com.vinakili.app.i18n.LocalStr
import com.vinakili.app.i18n.Str
import com.vinakili.app.i18n.strFor
import com.vinakili.app.screens.BalanceScreen
import com.vinakili.app.screens.ClientDetailScreen
import com.vinakili.app.screens.ClientsScreen
import com.vinakili.app.screens.DashboardScreen
import com.vinakili.app.screens.ExpensesScreen
import com.vinakili.app.screens.GoalsScreen
import com.vinakili.app.screens.InvoiceDetailScreen
import com.vinakili.app.screens.InvoiceEditorScreen
import com.vinakili.app.screens.InvoicesScreen
import com.vinakili.app.screens.LanguagePicker
import com.vinakili.app.screens.ListsScreen
import com.vinakili.app.screens.ProductsScreen
import com.vinakili.app.screens.ReportsScreen
import com.vinakili.app.screens.SettingsScreen
import com.vinakili.app.screens.SpendScreen
import com.vinakili.app.screens.StatsScreen
import com.vinakili.app.ui.AnimatedMoney
import com.vinakili.app.ui.BoxScopeToastHost
import com.vinakili.app.ui.LocalB
import com.vinakili.app.ui.OverlayBus
import com.vinakili.app.ui.SplashScreen
import com.vinakili.app.ui.VinakiliMark
import com.vinakili.app.ui.VinakiliTheme
import com.vinakili.app.ui.accent
import com.vinakili.app.ui.liquidGlass
import com.vinakili.app.ui.noRippleClickable
import kotlinx.coroutines.delay

data class TabDef(val label: String, val icon: ImageVector)

private data class ScreenKey(val business: Boolean, val tab: Int, val screen: Screen) {
    /** Linear position for deciding slide direction: root tabs first, overlays after. */
    val order: Int get() = when (screen) {
        is Screen.Root -> (if (business) 100 else 0) + tab
        else -> 1000
    }
}

@Composable
fun VinakiliApp(app: AppState) {
    val lang by Repo.lang.collectAsState()
    val theme by Repo.theme.collectAsState()
    val dark = theme != "light"
    val s = strFor(lang)

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1350); revealed = true }

    VinakiliTheme(dark = dark, business = app.business) {
        CompositionLocalProvider(LocalStr provides s) {
            val b = LocalB.current
            Box(Modifier.fillMaxSize().background(b.bg)) {
                if (lang == null) {
                    LanguagePicker(onPick = { Repo.setLang(it) })
                } else {
                    MainShell(app, s)
                }

                LaunchedEffect(app.toast) {
                    if (app.toast != null) { delay(2200); app.toast = null }
                }
                BoxScopeToastHost(app.toast, Modifier.align(Alignment.TopCenter).statusBarsPadding())

                AnimatedVisibility(
                    visible = !revealed,
                    enter = EnterTransition.None,
                    exit = fadeOut(tween(480)) + scaleOut(targetScale = 1.08f, animationSpec = tween(480)),
                ) {
                    SplashScreen()
                }
            }
        }
    }
}

@Composable
private fun MainShell(app: AppState, s: Str) {
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
            Box(Modifier.fillMaxSize()) {
                // Directional slide + fade: a small horizontal translate (a fraction of
                // the width) plus a crossfade — cheap (just a graphicsLayer offset) yet
                // gives a clear, smooth in/out sense of moving forward or back.
                AnimatedContent(
                    targetState = ScreenKey(app.business, tabIndex, app.current),
                    transitionSpec = {
                        val forward = targetState.order >= initialState.order
                        val dir = if (forward) 1 else -1
                        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { w -> dir * w / 5 } +
                            fadeIn(tween(220)))
                            .togetherWith(
                                slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { w -> -dir * w / 5 } +
                                    fadeOut(tween(180))
                            )
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
        }
        AnimatedVisibility(
            app.current is Screen.Root && OverlayBus.count == 0,
            enter = fadeIn(tween(220)) + slideInVertically(
                spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)) { it / 2 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            DockNav(app, tabs, tabIndex)
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
    // Left: brand.  Right: balance pill + settings, grouped and baseline-aligned,
    // so nothing sits dead-centre under a camera notch.
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VinakiliMark(30.dp)
            Spacer(Modifier.width(9.dp))
            Text("Vinakili", color = b.text, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BalancePill(app, s)
            Box(
                Modifier.size(42.dp)
                    .liquidGlass(b, 100.dp, elevation = 6.dp)
                    .noRippleClickable { if (app.current !is Screen.Settings) app.push(Screen.Settings) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Settings, s.settings, tint = b.textDim, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Compact liquid-glass balance chip — one line, 42dp tall to match the gear; opens Balance. */
@Composable
private fun BalancePill(app: AppState, s: Str) {
    val b = LocalB.current
    val balances by Repo.balances.flow.collectAsState()
    val total = balances.filter { it.deletedAt == null }.sumOf { it.amount }
    Row(
        Modifier
            .height(42.dp)
            .liquidGlass(b, 100.dp, glow = b.amber, elevation = 6.dp)
            .noRippleClickable {
                app.business = false
                app.personalTab = 2
                app.stack.clear()
            }
            .padding(start = 12.dp, end = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .background(b.amber.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AccountBalanceWallet, s.balance, tint = b.amber, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(9.dp))
        AnimatedMoney(total, color = b.text, fontSize = 15.sp, compact = true)
    }
}

/**
 * Floating liquid-glass dock. The active highlight is a fixed-size pill that
 * sits behind the ICON only and glides between tabs; labels are always shown
 * beneath each icon and constrained to their slot, so nothing spills out.
 */
@Composable
private fun DockNav(app: AppState, tabs: List<TabDef>, tabIndex: Int) {
    val b = LocalB.current
    val accent = b.accent(app.business)
    val iconRow = 38.dp
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp).padding(bottom = 10.dp)) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().liquidGlass(b, 28.dp, elevation = 16.dp).padding(horizontal = 6.dp, vertical = 8.dp),
        ) {
            val itemW = maxWidth / tabs.size
            val pillW = if (itemW < 54.dp) itemW - 6.dp else 52.dp
            val indicatorX by animateDpAsState(
                itemW * tabIndex + (itemW - pillW) / 2,
                spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                label = "dock",
            )
            // moving highlight behind the active icon
            Box(
                Modifier
                    .offset(x = indicatorX)
                    .width(pillW)
                    .height(iconRow)
                    .background(
                        Brush.verticalGradient(listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.12f))),
                        RoundedCornerShape(50),
                    )
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(50)),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { i, tab ->
                    val sel = i == tabIndex
                    val scale by animateFloatAsState(
                        if (sel) 1.12f else 1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "icon",
                    )
                    val tint by animateColorAsState(if (sel) accent else b.muted, tween(200), label = "tint")
                    Column(
                        Modifier.weight(1f)
                            .noRippleClickable {
                                if (app.business) app.businessTab = i else app.personalTab = i
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.height(iconRow), contentAlignment = Alignment.Center) {
                            Icon(tab.icon, tab.label, tint = tint,
                                modifier = Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale })
                        }
                        Text(
                            tab.label,
                            color = tint,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 8.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 3.dp, start = 2.dp, end = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
