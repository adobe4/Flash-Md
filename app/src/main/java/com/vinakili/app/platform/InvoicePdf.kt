package com.vinakili.app.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Base64
import com.vinakili.app.data.BizSettings
import com.vinakili.app.data.Client
import com.vinakili.app.data.Invoice
import com.vinakili.app.data.InvoiceItem
import com.vinakili.app.data.InvoiceTotals
import com.vinakili.app.data.Payment
import com.vinakili.app.data.fmtDate
import com.vinakili.app.data.money
import java.io.ByteArrayOutputStream

/** Renders a clean A4 invoice to a PDF byte array. Amber accent, no green/purple. */
object InvoicePdf {
    private const val PAGE_W = 595 // A4 @72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 42f

    private const val AMBER = 0xFFF59E0B.toInt()
    private const val INK = 0xFF17202E.toInt()
    private const val DIM = 0xFF5B6678.toInt()
    private const val LINE = 0xFFE2E6EE.toInt()
    private const val ZEBRA = 0xFFF6F8FB.toInt()

    fun build(
        inv: Invoice,
        client: Client?,
        items: List<InvoiceItem>,
        totals: InvoiceTotals,
        payments: List<Payment>,
        settings: BizSettings,
        statusLabel: String,
    ): ByteArray {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val c = page.canvas

        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val reg = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT }

        var y = MARGIN + 8f

        // ---- header band: business identity (left) + INVOICE title (right) ----
        val logo = decodeLogo(settings.logoBase64)
        var leftX = MARGIN
        if (logo != null) {
            val target = 46f
            val ratio = logo.width.toFloat() / logo.height.toFloat()
            val w = target * ratio
            c.drawBitmap(logo, null, Rect(MARGIN.toInt(), y.toInt(), (MARGIN + w).toInt(), (y + target).toInt()), null)
            leftX = MARGIN
            y += target + 12f
        }
        bold.color = INK; bold.textSize = 18f
        c.drawText(settings.name.ifBlank { "Vinakili" }, leftX, y, bold)
        reg.color = DIM; reg.textSize = 9.5f
        var infoY = y + 15f
        settings.address.split("\n").filter { it.isNotBlank() }.forEach {
            c.drawText(it, leftX, infoY, reg); infoY += 12f
        }
        if (settings.phone.isNotBlank()) { c.drawText(settings.phone, leftX, infoY, reg); infoY += 12f }
        if (settings.email.isNotBlank()) { c.drawText(settings.email, leftX, infoY, reg); infoY += 12f }

        // right: INVOICE + number + status pill
        bold.color = AMBER; bold.textSize = 26f
        bold.textAlign = Paint.Align.RIGHT
        c.drawText("INVOICE", PAGE_W - MARGIN, MARGIN + 20f, bold)
        bold.textSize = 11f; bold.color = INK
        c.drawText(inv.number, PAGE_W - MARGIN, MARGIN + 38f, bold)
        // status pill
        val pillText = statusLabel.uppercase()
        reg.textAlign = Paint.Align.RIGHT; reg.textSize = 9f; reg.color = DIM
        c.drawText("Status: $pillText", PAGE_W - MARGIN, MARGIN + 54f, reg)
        bold.textAlign = Paint.Align.LEFT
        reg.textAlign = Paint.Align.LEFT

        y = maxOf(infoY, MARGIN + 70f) + 14f

        // divider
        val linePaint = Paint().apply { color = LINE; strokeWidth = 1f }
        c.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 22f

        // ---- bill to + dates ----
        reg.color = DIM; reg.textSize = 9f
        c.drawText("BILL TO", MARGIN, y, reg)
        bold.color = INK; bold.textSize = 12f
        c.drawText(client?.name ?: inv.clientName.ifBlank { "—" }, MARGIN, y + 16f, bold)
        reg.color = DIM; reg.textSize = 9.5f
        var by = y + 30f
        client?.let { cl ->
            listOf(cl.phone, cl.email, cl.address).filter { it.isNotBlank() }.forEach {
                c.drawText(it, MARGIN, by, reg); by += 12f
            }
        }
        // dates on the right
        reg.textAlign = Paint.Align.RIGHT
        reg.color = DIM; reg.textSize = 9f
        c.drawText("Invoice date", PAGE_W - MARGIN, y, reg)
        bold.color = INK; bold.textSize = 10.5f; bold.textAlign = Paint.Align.RIGHT
        c.drawText(fmtDate(inv.date, "en"), PAGE_W - MARGIN, y + 14f, bold)
        reg.color = DIM; reg.textSize = 9f
        c.drawText("Due date", PAGE_W - MARGIN, y + 32f, reg)
        bold.color = INK; bold.textSize = 10.5f
        c.drawText(fmtDate(inv.dueDate, "en"), PAGE_W - MARGIN, y + 46f, bold)
        reg.textAlign = Paint.Align.LEFT; bold.textAlign = Paint.Align.LEFT

        y = maxOf(by, y + 52f) + 16f

        // ---- items table ----
        val colDesc = MARGIN
        val colQty = PAGE_W - MARGIN - 250f
        val colPrice = PAGE_W - MARGIN - 150f
        val colAmt = PAGE_W - MARGIN
        // header row
        val headBg = Paint().apply { color = INK }
        c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 24f, headBg)
        bold.color = Color.WHITE; bold.textSize = 9.5f
        c.drawText("DESCRIPTION", colDesc + 10f, y + 16f, bold)
        bold.textAlign = Paint.Align.RIGHT
        c.drawText("QTY", colQty + 30f, y + 16f, bold)
        c.drawText("UNIT PRICE", colPrice + 70f, y + 16f, bold)
        c.drawText("AMOUNT", colAmt - 10f, y + 16f, bold)
        bold.textAlign = Paint.Align.LEFT
        y += 24f

        val liveItems = items.filter { it.deletedAt == null && it.invoiceId == inv.id }
        liveItems.forEachIndexed { i, it ->
            val rowH = 22f
            if (i % 2 == 1) {
                c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + rowH, Paint().apply { color = ZEBRA })
            }
            reg.color = INK; reg.textSize = 10f
            c.drawText(clip(it.description, 46), colDesc + 10f, y + 15f, reg)
            reg.textAlign = Paint.Align.RIGHT
            c.drawText(trimNum(it.qty), colQty + 30f, y + 15f, reg)
            c.drawText(money(it.unitPrice), colPrice + 70f, y + 15f, reg)
            bold.color = INK; bold.textSize = 10f; bold.textAlign = Paint.Align.RIGHT
            c.drawText(money(it.subtotal), colAmt - 10f, y + 15f, bold)
            reg.textAlign = Paint.Align.LEFT; bold.textAlign = Paint.Align.LEFT
            y += rowH
        }
        c.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 18f

        // ---- totals block (right aligned) ----
        val totX = PAGE_W - MARGIN
        val labX = PAGE_W - MARGIN - 200f
        fun totalLine(label: String, value: String, emphasize: Boolean = false) {
            if (emphasize) {
                bold.color = INK; bold.textSize = 13f
                bold.textAlign = Paint.Align.LEFT
                c.drawText(label, labX, y, bold)
                bold.color = AMBER; bold.textAlign = Paint.Align.RIGHT
                c.drawText(value, totX, y, bold)
            } else {
                reg.color = DIM; reg.textSize = 10f; reg.textAlign = Paint.Align.LEFT
                c.drawText(label, labX, y, reg)
                reg.color = INK; reg.textAlign = Paint.Align.RIGHT
                c.drawText(value, totX, y, reg)
            }
            reg.textAlign = Paint.Align.LEFT; bold.textAlign = Paint.Align.LEFT
            y += 18f
        }
        totalLine("Subtotal", money(totals.subtotal))
        if (totals.discount > 0) totalLine("Discount", "- " + money(totals.discount))
        if (totals.vat > 0) totalLine("VAT (${trimNum(inv.vatPct)}%)", money(totals.vat))
        y += 4f
        c.drawLine(labX, y - 12f, totX, y - 12f, linePaint)
        totalLine("GRAND TOTAL", money(totals.total), emphasize = true)
        if (totals.paid > 0) {
            totalLine("Paid", "- " + money(totals.paid))
            totalLine("Balance due", money(totals.balance), emphasize = true)
        }

        y += 16f
        // ---- notes / payment info ----
        val notes = inv.notes.ifBlank { settings.paymentNotes }
        if (notes.isNotBlank()) {
            reg.color = DIM; reg.textSize = 9f
            c.drawText("NOTES / PAYMENT", MARGIN, y, reg)
            y += 14f
            reg.color = INK; reg.textSize = 10f
            wrap(notes, 92).forEach { c.drawText(it, MARGIN, y, reg); y += 13f }
        }

        // footer
        reg.color = DIM; reg.textSize = 8.5f; reg.textAlign = Paint.Align.CENTER
        c.drawText("Generated with Vinakili", PAGE_W / 2f, PAGE_H - 26f, reg)
        reg.textAlign = Paint.Align.LEFT

        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun decodeLogo(b64: String): Bitmap? {
        if (b64.isBlank()) return null
        return try {
            val clean = b64.substringAfter("base64,", b64)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) { null }
    }

    private fun clip(s: String, n: Int) = if (s.length <= n) s else s.take(n - 1) + "…"
    private fun trimNum(v: Double) = if (v == Math.floor(v)) v.toInt().toString() else v.toString()
    private fun wrap(s: String, width: Int): List<String> {
        val words = s.replace("\n", " ").split(" ")
        val lines = ArrayList<String>()
        var cur = StringBuilder()
        for (w in words) {
            if (cur.length + w.length + 1 > width) { lines.add(cur.toString()); cur = StringBuilder() }
            if (cur.isNotEmpty()) cur.append(" ")
            cur.append(w)
        }
        if (cur.isNotEmpty()) lines.add(cur.toString())
        return lines
    }
}
