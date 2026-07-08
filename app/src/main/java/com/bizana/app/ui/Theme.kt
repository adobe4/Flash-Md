package com.bizana.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Bizana palette — deliberately no green, no purple anywhere.
// Personal accent: amber. Business accent: blue.

@Immutable
data class BColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val hairline: Color,
    val text: Color,
    val textDim: Color,
    val muted: Color,
    val amber: Color,
    val onAmber: Color,
    val blue: Color,
    val onBlue: Color,
    val danger: Color,
    val onDanger: Color,
    // invoice / status colors (no green: "paid" is blue)
    val stPaid: Color,
    val stSent: Color,
    val stDraft: Color,
    val stOverdue: Color,
    val stCancelled: Color,
    // validated categorical chart series (dataviz-checked against surface)
    val series: List<Color>,
    val seriesNeutral: Color,
    // user-pickable balance colors
    val palette: List<Color>,
    val gridline: Color,
    val scrim: Color,
)

val DarkB = BColors(
    isDark = true,
    bg = Color(0xFF0C121D),
    surface = Color(0xFF141B26),
    surface2 = Color(0xFF1C2534),
    hairline = Color(0x17FFFFFF),
    text = Color(0xFFF2F5FA),
    textDim = Color(0xFF9AA7B8),
    muted = Color(0xFF64748B),
    amber = Color(0xFFF59E0B),
    onAmber = Color(0xFF221300),
    blue = Color(0xFF3B82F6),
    onBlue = Color(0xFFFFFFFF),
    danger = Color(0xFFEF4444),
    onDanger = Color(0xFFFFFFFF),
    stPaid = Color(0xFF3B82F6),
    stSent = Color(0xFFF59E0B),
    stDraft = Color(0xFF94A3B8),
    stOverdue = Color(0xFFEF4444),
    stCancelled = Color(0xFF6B7280),
    series = listOf(
        Color(0xFFC17A00), Color(0xFF3B82F6), Color(0xFFE11D48),
        Color(0xFF0284C7), Color(0xFFDB2777), Color(0xFFB45309),
    ),
    seriesNeutral = Color(0xFF64748B),
    palette = listOf(
        Color(0xFFF59E0B), Color(0xFF3B82F6), Color(0xFF0EA5E9), Color(0xFFF43F5E),
        Color(0xFFEC4899), Color(0xFFF97316), Color(0xFF94A3B8), Color(0xFFEF4444),
    ),
    gridline = Color(0x14FFFFFF),
    scrim = Color(0x99000000),
)

val LightB = BColors(
    isDark = false,
    bg = Color(0xFFF1F4F9),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF4F6FA),
    hairline = Color(0x14101828),
    text = Color(0xFF17202E),
    textDim = Color(0xFF5B6678),
    muted = Color(0xFF8A94A6),
    amber = Color(0xFFD97706),
    onAmber = Color(0xFFFFFFFF),
    blue = Color(0xFF2563EB),
    onBlue = Color(0xFFFFFFFF),
    danger = Color(0xFFDC2626),
    onDanger = Color(0xFFFFFFFF),
    stPaid = Color(0xFF2563EB),
    stSent = Color(0xFFB45309),
    stDraft = Color(0xFF64748B),
    stOverdue = Color(0xFFDC2626),
    stCancelled = Color(0xFF6B7280),
    series = listOf(
        Color(0xFFB45309), Color(0xFF2563EB), Color(0xFFBE123C),
        Color(0xFF0369A1), Color(0xFFBE185D), Color(0xFF92400E),
    ),
    seriesNeutral = Color(0xFF64748B),
    palette = listOf(
        Color(0xFFD97706), Color(0xFF2563EB), Color(0xFF0284C7), Color(0xFFE11D48),
        Color(0xFFDB2777), Color(0xFFEA580C), Color(0xFF64748B), Color(0xFFDC2626),
    ),
    gridline = Color(0x12101828),
    scrim = Color(0x66000000),
)

val LocalB = staticCompositionLocalOf { DarkB }

/** Accent for the active section. */
fun BColors.accent(business: Boolean): Color = if (business) blue else amber
fun BColors.onAccent(business: Boolean): Color = if (business) onBlue else onAmber

fun BColors.statusColor(status: String): Color = when (status) {
    "paid" -> stPaid
    "sent" -> stSent
    "overdue" -> stOverdue
    "cancelled" -> stCancelled
    else -> stDraft
}

@Composable
fun BizanaTheme(dark: Boolean = isSystemInDarkTheme(), business: Boolean = false, content: @Composable () -> Unit) {
    val b = if (dark) DarkB else LightB
    val accent = b.accent(business)
    val scheme = if (dark) darkColorScheme(
        primary = accent,
        onPrimary = b.onAccent(business),
        background = b.bg,
        onBackground = b.text,
        surface = b.surface,
        onSurface = b.text,
        surfaceVariant = b.surface2,
        onSurfaceVariant = b.textDim,
        surfaceContainerHigh = b.surface2,
        surfaceContainerLow = b.surface,
        outline = b.muted,
        error = b.danger,
    ) else lightColorScheme(
        primary = accent,
        onPrimary = b.onAccent(business),
        background = b.bg,
        onBackground = b.text,
        surface = b.surface,
        onSurface = b.text,
        surfaceVariant = b.surface2,
        onSurfaceVariant = b.textDim,
        surfaceContainerHigh = b.surface2,
        surfaceContainerLow = b.surface,
        outline = b.muted,
        error = b.danger,
    )
    CompositionLocalProvider(LocalB provides b) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
