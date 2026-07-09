package com.vinakili.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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

private data class ScreenKey(val business: Boolean, val tab: Int, val screen: Screen)

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
                AnimatedContent(
                    targetState = ScreenKey(app.business, tabIndex, app.current),
                    transitionSpec = {
                        (fadeIn(tween(240)) + scaleIn(initialScale = 0.975f, animationSpec = tween(240)))
                            .togetherWith(fadeOut(tween(160)))
                            .using(SizeTransform(clip = false))
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
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        VinakiliMark(34.dp)

        BalancePill(app, s)

        Box(
            Modifier.size(40.dp)
                .liquidGlass(b, 100.dp, elevation = 8.dp)
                .noRippleClickable { if (app.current !is Screen.Settings) app.push(Screen.Settings) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Settings, s.settings, tint = b.textDim, modifier = Modifier.size(20.dp))
        }
    }
}

/** Liquid-glass pill in the notch area showing total balance; tapping opens the Balance tab. */
@Composable
private fun BalancePill(app: AppState, s: Str) {
    val b = LocalB.current
    val balances by Repo.balances.flow.collectAsState()
    val total = balances.filter { it.deletedAt == null }.sumOf { it.amount }
    Row(
        Modifier
            .liquidGlass(b, 24.dp, glow = b.amber, elevation = 10.dp)
            .noRippleClickable {
                app.business = false
                app.personalTab = 2
                app.stack.clear()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AccountBalanceWallet, s.balance, tint = b.amber, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(s.balance, color = b.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp)
            AnimatedMoney(total, color = b.text, fontSize = 14.sp, compact = true)
        }
    }
}

/** Floating liquid-glass dock with a spring-morphing active pill. */
@Composable
private fun DockNav(app: AppState, tabs: List<TabDef>, tabIndex: Int) {
    val b = LocalB.current
    val accent = b.accent(app.business)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 10.dp)) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().liquidGlass(b, 30.dp, elevation = 18.dp).padding(6.dp),
        ) {
            val itemW = maxWidth / tabs.size
            val indicatorX by animateDpAsState(
                itemW * tabIndex,
                spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                label = "dock",
            )
            Box(
                Modifier
                    .offset(x = indicatorX)
                    .width(itemW)
                    .height(54.dp)
                    .padding(horizontal = 3.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.10f))
                        ),
                        RoundedCornerShape(24.dp),
                    )
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { i, tab ->
                    val sel = i == tabIndex
                    val scale by animateFloatAsState(
                        if (sel) 1.15f else 1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "icon",
                    )
                    val tint by animateColorAsState(if (sel) accent else b.muted, tween(200), label = "tint")
                    Column(
                        Modifier.weight(1f).height(54.dp)
                            .noRippleClickable {
                                if (app.business) app.businessTab = i else app.personalTab = i
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(tab.icon, tab.label, tint = tint,
                            modifier = Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale })
                        AnimatedVisibility(
                            sel,
                            enter = fadeIn(tween(180)) + expandVertically(
                                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)),
                            exit = fadeOut(tween(100)) + shrinkVertically(tween(120)),
                        ) {
                            Text(tab.label, color = accent, fontWeight = FontWeight.Bold,
                                fontSize = 9.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
