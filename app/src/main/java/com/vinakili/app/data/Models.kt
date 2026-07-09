package com.vinakili.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

fun uuid(): String = UUID.randomUUID().toString()

fun nowIso(): String {
    val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    f.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return f.format(Date())
}

fun todayStr(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun ymOf(date: String): String = if (date.length >= 7) date.substring(0, 7) else ""

fun thisYm(): String = todayStr().substring(0, 7)

fun thisYear(): String = todayStr().substring(0, 4)

fun addDays(date: String, days: Int): String {
    return try {
        val cal = Calendar.getInstance()
        cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: Date()
        cal.add(Calendar.DAY_OF_MONTH, days)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    } catch (_: Exception) { date }
}

/** Days from [a] to [b]; positive when b is after a. */
fun daysBetween(a: String, b: String): Int {
    return try {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val da = f.parse(a) ?: return 0
        val db = f.parse(b) ?: return 0
        ((db.time - da.time) / 86_400_000L).toInt()
    } catch (_: Exception) { 0 }
}

/** Last [n] month keys ("yyyy-MM"), oldest first. */
fun lastMonthKeys(n: Int): List<String> {
    val out = ArrayList<String>(n)
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.add(Calendar.MONTH, -(n - 1))
    val f = SimpleDateFormat("yyyy-MM", Locale.US)
    repeat(n) {
        out.add(f.format(cal.time))
        cal.add(Calendar.MONTH, 1)
    }
    return out
}

fun monthShort(ym: String, lang: String): String {
    return try {
        val d = SimpleDateFormat("yyyy-MM", Locale.US).parse(ym) ?: return ym
        SimpleDateFormat("MMM", if (lang == "sw") Locale("sw") else Locale.ENGLISH).format(d)
    } catch (_: Exception) { ym }
}

fun fmtDate(date: String?, lang: String): String {
    if (date.isNullOrBlank()) return "—"
    return try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date.take(10)) ?: return date
        SimpleDateFormat("d MMM yyyy", if (lang == "sw") Locale("sw") else Locale.ENGLISH).format(d)
    } catch (_: Exception) { date }
}

fun fmtTzs(n: Double): String {
    val v = Math.round(n)
    return String.format(Locale.US, "%,d", v)
}

fun money(n: Double): String = "TZS " + fmtTzs(n)

fun moneyCompact(n: Double): String {
    val v = Math.abs(n)
    return when {
        v >= 1_000_000 -> {
            val m = n / 1_000_000.0
            (if (m == Math.floor(m)) String.format(Locale.US, "%.0f", m) else String.format(Locale.US, "%.1f", m)) + "M"
        }
        v >= 10_000 -> String.format(Locale.US, "%,d", Math.round(n / 1000.0)) + "k"
        else -> fmtTzs(n)
    }
}

// ---------- Personal ----------

@Serializable
data class PersonalItem(
    val id: String = uuid(),
    /** "debt" | "buy" | "spend" */
    val type: String = "debt",
    val name: String = "",
    val amount: Double = 0.0,
    val done: Boolean = false,
    @SerialName("created_at") val createdAt: String = nowIso(),
    @SerialName("updated_at") val updatedAt: String = nowIso(),
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class BalanceEntry(
    val id: String = uuid(),
    val where: String = "",
    val amount: Double = 0.0,
    /** index into the balance palette */
    val color: Int = 0,
    @SerialName("created_at") val createdAt: String = nowIso(),
    @SerialName("updated_at") val updatedAt: String = nowIso(),
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class Goal(
    val id: String = uuid(),
    /** "money" | "stuff" */
    val kind: String = "money",
    val name: String = "",
    val target: Double = 0.0,
    val current: Double = 0.0,
    val price: Double = 0.0,
    @SerialName("due_month") val dueMonth: String = "",
    val done: Boolean = false,
    @SerialName("created_at") val createdAt: String = nowIso(),
    @SerialName("updated_at") val updatedAt: String = nowIso(),
    @SerialName("deleted_at") val deletedAt: String? = null,
)

// ---------- Business ----------

@Serializable
data class Client(
    val id: String = uuid(),
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    @SerialName("created_at") val createdAt: String = nowIso(),
    @SerialName("updated_at") val updatedAt: String = nowIso(),
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class StatusChange(
    val status: String = "",
    val at: String = nowIso(),
)

/** status: draft | sent | paid | cancelled */
@Serializable
data class Invoice(
    val id: String = uuid(),
    val number: String = "",
    @SerialName("client_id") val clientId: String = "",
    @SerialName("client_name") val clientName: String = "",
    val date: String = todayStr(),
    @SerialName("due_date") val dueDate: String = todayStr(),
    /** "pct" | "amt" */
    @SerialName("discount_type") val discountType: String = "pct",
    @SerialName("discount_value") val discountValue: Double = 0.0,
    @SerialName("vat_pct") val vatPct: Double = 0.0,
    val notes: String = "",
    val status: String = "draft",
    @SerialName("status_history") val statusHistory: List<StatusChange> = emptyList(),
    @SerialName("created_at") val createdAt: String = nowIso(),
    @SerialName("updated_at") val updatedAt: String = nowIso(),
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class InvoiceItem(
    val id: String = uuid(),
    @SerialName("invoice_id") val invoiceId: String = "",
    val description: String = "",
    val qty: Double = 1.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("created_at") val createdAt: String = nowIso(),
    @SerialName("updated_at") val updatedAt: String = nowIso(),
    @SerialName("deleted_at") val deletedAt: String? = null,
) {
    val subtotal: Double get() = qty * unitPrice
}

@Serializable
data class Payment(
    val id: String = uuid(),
    @SerialName("invoice_id") val invoiceId: String = "",
    val amount: Double = 0.0,
    val date: String = todayStr(),
    /** cash | mpesa | bank | other */
    val method: String = "cash",
    val note: String = "",
    @SerialName("created_at") val createdAt: String = nowIso(),
    @SerialName("updated_at") val updatedAt: String = nowIso(),
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class Product(
    val id: String = uuid(),
    val name: String = "",
    val description: String = "",
    /** piece | hour | kg | month | service */
    val unit: String = "piece",
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    /** "product" | "service" */
    val category: String = "product",
    @SerialName("created_at") val createdAt: String = nowIso(),
    @SerialName("updated_at") val updatedAt: String = nowIso(),
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class Expense(
    val id: String = uuid(),
    val what: String = "",
    /** stock | transport | rent | salaries | marketing | utilities | other */
    val category: String = "other",
    val amount: Double = 0.0,
    val date: String = todayStr(),
    val notes: String = "",
    @SerialName("created_at") val createdAt: String = nowIso(),
    @SerialName("updated_at") val updatedAt: String = nowIso(),
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class BizSettings(
    val name: String = "",
    @SerialName("logo_base64") val logoBase64: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    @SerialName("invoice_prefix") val invoicePrefix: String = "INV",
    @SerialName("default_vat") val defaultVat: Double = 0.0,
    @SerialName("payment_notes") val paymentNotes: String = "",
    @SerialName("updated_at") val updatedAt: String = nowIso(),
)

// ---------- Derived invoice math ----------

data class InvoiceTotals(
    val subtotal: Double,
    val discount: Double,
    val vat: Double,
    val total: Double,
    val paid: Double,
    val balance: Double,
)

fun invoiceTotals(inv: Invoice, items: List<InvoiceItem>, payments: List<Payment>): InvoiceTotals {
    val sub = items.filter { it.deletedAt == null && it.invoiceId == inv.id }.sumOf { it.subtotal }
    val disc = if (inv.discountType == "pct") sub * (inv.discountValue / 100.0) else inv.discountValue
    val afterDisc = (sub - disc).coerceAtLeast(0.0)
    val vat = afterDisc * (inv.vatPct / 100.0)
    val total = afterDisc + vat
    val paid = payments.filter { it.deletedAt == null && it.invoiceId == inv.id }.sumOf { it.amount }
    return InvoiceTotals(sub, disc, vat, total, paid, (total - paid).coerceAtLeast(0.0))
}

/** Effective display status — sent + past due + unpaid balance = overdue. */
fun effectiveStatus(inv: Invoice, totals: InvoiceTotals): String {
    if (inv.status == "sent" && totals.balance > 0.0 && inv.dueDate < todayStr()) return "overdue"
    return inv.status
}
