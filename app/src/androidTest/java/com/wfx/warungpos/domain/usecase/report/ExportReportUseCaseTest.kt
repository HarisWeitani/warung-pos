package com.wfx.warungpos.domain.usecase.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wfx.warungpos.core.common.ExpenseCategory
import com.wfx.warungpos.domain.model.BestSeller
import com.wfx.warungpos.domain.model.PaymentBreakdown
import com.wfx.warungpos.domain.model.ReportData
import com.wfx.warungpos.domain.model.VoidStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportReportUseCaseTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val useCase = ExportReportUseCase(context)

    private val data = ReportData(
        revenue = 100_000L,
        expenses = 20_000L,
        grossProfit = 80_000L,
        paymentBreakdown = listOf(PaymentBreakdown("cash", 100_000L)),
        voidStats = VoidStats(count = 1, totalValue = 5_000L),
        expensesByCategory = mapOf(ExpenseCategory.SUPPLIES to 20_000L),
        bestSellers = listOf(BestSeller("item-1", "EsTeh", totalQty = 3, totalRevenue = 15_000L)),
    )

    @Test
    fun csvExport_producesReadableCsvFile() {
        val uri = useCase(data, "DAY", ReportExportFormat.CSV)

        assertEquals("content", uri.scheme)
        context.contentResolver.openInputStream(uri)!!.use { stream ->
            val text = stream.readBytes().decodeToString()
            assertTrue(text.contains("Revenue,100000"))
            assertTrue(text.contains("EsTeh"))
        }
    }

    @Test
    fun pdfExport_producesNonEmptyPdfFile() {
        val uri = useCase(data, "DAY", ReportExportFormat.PDF)

        assertEquals("content", uri.scheme)
        context.contentResolver.openInputStream(uri)!!.use { stream ->
            val bytes = stream.readBytes()
            assertTrue(bytes.size > 100)
            assertEquals("%PDF", bytes.decodeToString(0, 4))
        }
    }
}
