package com.wfx.warungpos.domain.usecase.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.wfx.warungpos.core.util.CurrencyFormatter
import com.wfx.warungpos.domain.model.ReportData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

enum class ReportExportFormat(val extension: String, val mimeType: String) {
    CSV("csv", "text/csv"),
    PDF("pdf", "application/pdf"),
}

class ExportReportUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke(data: ReportData, rangeLabel: String, format: ReportExportFormat): Uri {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "warungpos_report_${System.currentTimeMillis()}.${format.extension}")
        when (format) {
            ReportExportFormat.CSV -> file.writeText(buildCsv(data, rangeLabel))
            ReportExportFormat.PDF -> writePdf(file, data, rangeLabel)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun buildCsv(data: ReportData, rangeLabel: String): String = buildString {
        appendLine("Warung POS Report,$rangeLabel")
        appendLine()
        appendLine("Revenue,${data.revenue}")
        appendLine("Expenses,${data.expenses}")
        appendLine("Gross Profit,${data.grossProfit}")
        appendLine()
        appendLine("Payment Method,Total")
        data.paymentBreakdown.forEach { appendLine("${it.paymentMethodId},${it.total}") }
        appendLine()
        appendLine("Expense Category,Total")
        data.expensesByCategory.forEach { (category, total) -> appendLine("${category.name},$total") }
        appendLine()
        appendLine("Void Count,${data.voidStats.count}")
        appendLine("Void Value,${data.voidStats.totalValue}")
        appendLine()
        appendLine("Item,Qty,Revenue")
        data.bestSellers.forEach { appendLine("${csvEscape(it.name)},${it.totalQty},${it.totalRevenue}") }
        appendLine()
        appendLine("# All monetary values in Rupiah. Formatted totals: revenue ${CurrencyFormatter.format(data.revenue)}, expenses ${CurrencyFormatter.format(data.expenses)}, gross profit ${CurrencyFormatter.format(data.grossProfit)}")
    }

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value

    // A4 at 72dpi
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 40f

    private fun writePdf(file: File, data: ReportData, rangeLabel: String) {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
        val headerPaint = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD }
        val bodyPaint = Paint().apply { textSize = 11f }
        val lineHeight = 18f

        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create())
        var canvas = page.canvas
        var y = margin

        fun newPageIfNeeded(extra: Float = lineHeight) {
            if (y + extra > pageHeight - margin) {
                document.finishPage(page)
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create())
                canvas = page.canvas
                y = margin
            }
        }

        fun drawLine(text: String, paint: Paint, gap: Float = lineHeight) {
            newPageIfNeeded(gap)
            canvas.drawText(text, margin, y, paint)
            y += gap
        }

        fun drawRow(label: String, value: String, gap: Float = lineHeight) {
            newPageIfNeeded(gap)
            canvas.drawText(label, margin, y, bodyPaint)
            canvas.drawText(value, pageWidth - margin - bodyPaint.measureText(value), y, bodyPaint)
            y += gap
        }

        drawLine("Warung POS Report", titlePaint, 26f)
        drawLine(rangeLabel, bodyPaint, 24f)

        drawLine("Summary", headerPaint, 20f)
        drawRow("Revenue", CurrencyFormatter.format(data.revenue))
        drawRow("Expenses", CurrencyFormatter.format(data.expenses))
        drawRow("Gross Profit", CurrencyFormatter.format(data.grossProfit))
        y += 10f

        if (data.paymentBreakdown.isNotEmpty()) {
            drawLine("Payment Methods", headerPaint, 20f)
            data.paymentBreakdown.forEach { drawRow(it.paymentMethodId, CurrencyFormatter.format(it.total)) }
            y += 10f
        }

        if (data.expensesByCategory.isNotEmpty()) {
            drawLine("Expenses by Category", headerPaint, 20f)
            data.expensesByCategory.forEach { (category, total) -> drawRow(category.name, CurrencyFormatter.format(total)) }
            y += 10f
        }

        drawLine("Void Summary", headerPaint, 20f)
        drawRow("Voided Items", data.voidStats.count.toString())
        drawRow("Voided Value", CurrencyFormatter.format(data.voidStats.totalValue))
        y += 10f

        if (data.bestSellers.isNotEmpty()) {
            drawLine("Best Sellers", headerPaint, 20f)
            data.bestSellers.forEach { drawRow("${it.name} (x${it.totalQty})", CurrencyFormatter.format(it.totalRevenue)) }
        }

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
    }
}
