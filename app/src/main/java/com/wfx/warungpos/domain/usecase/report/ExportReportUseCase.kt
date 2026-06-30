package com.wfx.warungpos.domain.usecase.report

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.wfx.warungpos.core.util.CurrencyFormatter
import com.wfx.warungpos.domain.model.ReportData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class ExportReportUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke(data: ReportData, rangeLabel: String): Uri {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "warungpos_report_${System.currentTimeMillis()}.csv")
        file.writeText(buildCsv(data, rangeLabel))
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
}
