package com.vinakili.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

/** One persisted list-of-rows table, mirrored in a StateFlow for Compose. */
class Table<T : Any>(
    private val fileName: String,
    private val ser: KSerializer<List<T>>,
    private val getId: (T) -> String,
    private val getUpdated: (T) -> String,
    private val markDeleted: (T, String) -> T,
) {
    val flow = MutableStateFlow<List<T>>(emptyList())
    private var dir: File? = null

    fun init(dir: File) {
        this.dir = dir
        val f = File(dir, fileName)
        if (f.exists()) {
            try {
                flow.value = json.decodeFromString(ser, f.readText())
            } catch (_: Exception) { /* corrupt file: start empty */ }
        }
    }

    private fun persist() {
        val d = dir ?: return
        try {
            File(d, fileName).writeText(json.encodeToString(ser, flow.value))
        } catch (_: Exception) { }
    }

    fun set(rows: List<T>) {
        flow.value = rows
        persist()
    }

    fun upsert(row: T) {
        val id = getId(row)
        val cur = flow.value
        val idx = cur.indexOfFirst { getId(it) == id }
        set(if (idx >= 0) cur.toMutableList().also { it[idx] = row } else cur + row)
    }

    fun softDelete(id: String) {
        set(flow.value.map { if (getId(it) == id) markDeleted(it, nowIso()) else it })
    }

    /** Merge incoming rows: newer updated_at wins per id. Returns count processed. */
    fun merge(incoming: List<T>): Int {
        val byId = LinkedHashMap<String, T>()
        for (r in flow.value) byId[getId(r)] = r
        for (r in incoming) {
            val mine = byId[getId(r)]
            if (mine == null || getUpdated(r) > getUpdated(mine)) byId[getId(r)] = r
        }
        set(byId.values.toList())
        return incoming.size
    }

    fun encode(): String = json.encodeToString(ser, flow.value)

    fun decodeAndReplace(raw: String): Int {
        val rows = json.decodeFromString(ser, raw)
        set(rows)
        return rows.size
    }

    fun decodeAndMerge(raw: String): Int = merge(json.decodeFromString(ser, raw))
}

object Repo {
    lateinit var prefs: SharedPreferences
        private set

    val personalItems = Table("personal_items.json", ListSerializer(PersonalItem.serializer()),
        { it.id }, { it.updatedAt }, { r, t -> r.copy(deletedAt = t, updatedAt = t) })
    val balances = Table("balances.json", ListSerializer(BalanceEntry.serializer()),
        { it.id }, { it.updatedAt }, { r, t -> r.copy(deletedAt = t, updatedAt = t) })
    val goals = Table("goals.json", ListSerializer(Goal.serializer()),
        { it.id }, { it.updatedAt }, { r, t -> r.copy(deletedAt = t, updatedAt = t) })
    val clients = Table("clients.json", ListSerializer(Client.serializer()),
        { it.id }, { it.updatedAt }, { r, t -> r.copy(deletedAt = t, updatedAt = t) })
    val invoices = Table("invoices.json", ListSerializer(Invoice.serializer()),
        { it.id }, { it.updatedAt }, { r, t -> r.copy(deletedAt = t, updatedAt = t) })
    val invoiceItems = Table("invoice_items.json", ListSerializer(InvoiceItem.serializer()),
        { it.id }, { it.updatedAt }, { r, t -> r.copy(deletedAt = t, updatedAt = t) })
    val payments = Table("payments.json", ListSerializer(Payment.serializer()),
        { it.id }, { it.updatedAt }, { r, t -> r.copy(deletedAt = t, updatedAt = t) })
    val products = Table("products.json", ListSerializer(Product.serializer()),
        { it.id }, { it.updatedAt }, { r, t -> r.copy(deletedAt = t, updatedAt = t) })
    val expenses = Table("expenses.json", ListSerializer(Expense.serializer()),
        { it.id }, { it.updatedAt }, { r, t -> r.copy(deletedAt = t, updatedAt = t) })

    val bizSettings = MutableStateFlow(BizSettings())

    /** null = not chosen yet (first launch language screen) */
    val lang = MutableStateFlow<String?>(null)
    val theme = MutableStateFlow("dark")

    private var dataDir: File? = null

    fun init(context: Context) {
        if (dataDir != null) return
        prefs = context.getSharedPreferences("vinakili", Context.MODE_PRIVATE)
        val dir = File(context.filesDir, "data").apply { mkdirs() }
        dataDir = dir
        listOf(personalItems, balances, goals, clients, invoices, invoiceItems, payments, products, expenses)
            .forEach { it.init(dir) }
        val bf = File(dir, "biz_settings.json")
        if (bf.exists()) {
            try { bizSettings.value = json.decodeFromString(BizSettings.serializer(), bf.readText()) } catch (_: Exception) { }
        }
        lang.value = prefs.getString("vinakili.lang", null)
        theme.value = prefs.getString("vinakili.theme", "dark") ?: "dark"
    }

    fun saveBizSettings(s: BizSettings) {
        val v = s.copy(updatedAt = nowIso())
        bizSettings.value = v
        dataDir?.let { File(it, "biz_settings.json").writeText(json.encodeToString(BizSettings.serializer(), v)) }
    }

    fun setLang(l: String) {
        lang.value = l
        prefs.edit().putString("vinakili.lang", l).apply()
    }

    fun setTheme(t: String) {
        theme.value = t
        prefs.edit().putString("vinakili.theme", t).apply()
    }

    fun nextInvoiceNumber(): String {
        val prefix = bizSettings.value.invoicePrefix.ifBlank { "INV" }
        val max = invoices.flow.value
            .mapNotNull { it.number.substringAfterLast("-").toIntOrNull() }
            .maxOrNull() ?: 0
        return prefix + "-" + String.format(java.util.Locale.US, "%04d", max + 1)
    }

    // ---------- Backup ----------

    const val BK_ITEMS = "vinakili.personal.items"
    const val BK_BALANCES = "vinakili.personal.balances"
    const val BK_GOALS = "vinakili.personal.goals"
    const val BK_CLIENTS = "vinakili.business.clients"
    const val BK_INVOICES = "vinakili.business.invoices"
    const val BK_INV_ITEMS = "vinakili.business.invoice_items"
    const val BK_PAYMENTS = "vinakili.business.payments"
    const val BK_PRODUCTS = "vinakili.business.products"
    const val BK_EXPENSES = "vinakili.business.expenses"
    const val BK_SETTINGS = "vinakili.business.settings"

    fun buildBackupJson(): String {
        val obj = buildJsonObject {
            put("app", "vinakili")
            put("version", 1)
            put("exported_at", nowIso())
            put("data", buildJsonObject {
                put(BK_ITEMS, json.parseToJsonElement(personalItems.encode()))
                put(BK_BALANCES, json.parseToJsonElement(balances.encode()))
                put(BK_GOALS, json.parseToJsonElement(goals.encode()))
                put(BK_CLIENTS, json.parseToJsonElement(clients.encode()))
                put(BK_INVOICES, json.parseToJsonElement(invoices.encode()))
                put(BK_INV_ITEMS, json.parseToJsonElement(invoiceItems.encode()))
                put(BK_PAYMENTS, json.parseToJsonElement(payments.encode()))
                put(BK_PRODUCTS, json.parseToJsonElement(products.encode()))
                put(BK_EXPENSES, json.parseToJsonElement(expenses.encode()))
                put(BK_SETTINGS, json.parseToJsonElement(json.encodeToString(BizSettings.serializer(), bizSettings.value)))
            })
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    /** mode: "replace" | "merge". Returns number of items loaded. Throws on invalid file. */
    fun restoreBackup(raw: String, mode: String): Int {
        val root = json.parseToJsonElement(raw).jsonObject
        val data = root["data"]?.jsonObject ?: root // tolerate bare data objects
        var count = 0
        fun <T : Any> apply(key: String, table: Table<T>) {
            val el = data[key] ?: return
            val s = json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), el)
            count += if (mode == "replace") table.decodeAndReplace(s) else table.decodeAndMerge(s)
        }
        apply(BK_ITEMS, personalItems)
        apply(BK_BALANCES, balances)
        apply(BK_GOALS, goals)
        apply(BK_CLIENTS, clients)
        apply(BK_INVOICES, invoices)
        apply(BK_INV_ITEMS, invoiceItems)
        apply(BK_PAYMENTS, payments)
        apply(BK_PRODUCTS, products)
        apply(BK_EXPENSES, expenses)
        data[BK_SETTINGS]?.let { el ->
            try {
                val incoming = json.decodeFromString(BizSettings.serializer(),
                    json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), el))
                if (mode == "replace" || incoming.updatedAt > bizSettings.value.updatedAt) saveBizSettings(incoming)
            } catch (_: Exception) { }
        }
        // sanity: reject files with nothing recognizable
        if (count == 0 && data.keys.none { it.startsWith("vinakili.") }) {
            throw IllegalArgumentException("not a vinakili backup")
        }
        return count
    }

    // ---------- CSV (Lists tab) ----------

    fun listsCsv(): String {
        val sb = StringBuilder("type,name,amount,done,created_at\n")
        for (r in personalItems.flow.value.filter { it.deletedAt == null && it.type != "spend" }) {
            val name = "\"" + r.name.replace("\"", "\"\"") + "\""
            sb.append("${r.type},$name,${r.amount},${r.done},${r.createdAt}\n")
        }
        return sb.toString()
    }

    fun importListsCsv(raw: String): Int {
        var n = 0
        val lines = raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines.drop(1)) {
            // naive CSV split honoring quoted name field
            val cols = parseCsvLine(line)
            if (cols.size < 3) continue
            val type = cols[0].trim().lowercase()
            if (type != "debt" && type != "buy") continue
            val amount = cols[2].toDoubleOrNull() ?: continue
            personalItems.upsert(PersonalItem(type = type, name = cols[1], amount = amount,
                done = cols.getOrNull(3)?.trim()?.equals("true", true) == true))
            n++
        }
        return n
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQ = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQ && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQ = !inQ
                c == ',' && !inQ -> { out.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}
