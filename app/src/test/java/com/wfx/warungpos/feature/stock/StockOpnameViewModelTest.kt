package com.wfx.warungpos.feature.stock

import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VarianceReason
import com.wfx.warungpos.domain.model.StockItem
import com.wfx.warungpos.domain.usecase.stock.StartStockOpnameUseCase
import com.wfx.warungpos.domain.usecase.stock.SubmitStockOpnameUseCase
import com.wfx.warungpos.fake.FakeSessionProvider
import com.wfx.warungpos.fake.FakeStockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** DEFECT-013: counted-quantity/reason edits used to live only in ViewModel state, so navigating
 * away and back (which recreates the ViewModel) silently discarded anything not yet submitted.
 * These tests simulate that recreation by constructing a *second* ViewModel instance against the
 * same (fake, but representative of the real Room-backed) repository. */
@OptIn(ExperimentalCoroutinesApi::class)
class StockOpnameViewModelTest {

    private lateinit var stockRepository: FakeStockRepository
    private lateinit var sessionProvider: FakeSessionProvider

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        stockRepository = FakeStockRepository()
        sessionProvider = FakeSessionProvider()
        stockRepository.items["stock-1"] = StockItem(
            id = "stock-1", name = "Rice", unit = "kg", currentQty = 10.0, reorderPoint = 2.0,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = StockOpnameViewModel(
        stockRepository,
        StartStockOpnameUseCase(stockRepository, sessionProvider),
        SubmitStockOpnameUseCase(stockRepository),
    )

    // FakeStockRepository's observeInProgressOpname() returns a fixed flowOf(...) snapshot
    // evaluated at call time, not a live-updating flow (unlike the real Room-backed Flow) — so a
    // ViewModel must be constructed *after* the opname already exists for its init block to see
    // it, exactly as production code sees it appear reactively via Room. Starting the opname
    // through the real use case (not through a ViewModel's own startOpname()) keeps this a
    // faithful test of the actual persist-and-reread bug/fix rather than working around the
    // fake's snapshot limitation.
    private suspend fun seedInProgressOpname() {
        StartStockOpnameUseCase(stockRepository, sessionProvider)().getOrThrow()
    }

    @Test
    fun `DEFECT-013 regression - a counted-qty edit survives ViewModel recreation`() = runTest {
        seedInProgressOpname()
        val firstVm = newViewModel()

        firstVm.onCountedQtyChange("stock-1", "15")
        assertEquals("15", firstVm.uiState.value.lines.first { it.stockItemId == "stock-1" }.countedQty)

        // Simulate navigating away and back: a fresh ViewModel instance against the same
        // (still in-progress) opname, exactly what happens when the screen's ViewModel is
        // recreated by the nav graph.
        val secondVm = newViewModel()

        assertEquals(
            "the counted qty typed before navigating away must still be there",
            "15",
            secondVm.uiState.value.lines.first { it.stockItemId == "stock-1" }.countedQty,
        )
    }

    @Test
    fun `DEFECT-013 regression - a variance reason edit survives ViewModel recreation`() = runTest {
        seedInProgressOpname()
        val firstVm = newViewModel()
        firstVm.onCountedQtyChange("stock-1", "8")
        firstVm.onReasonChange("stock-1", VarianceReason.DAMAGE)

        val secondVm = newViewModel()

        val line = secondVm.uiState.value.lines.first { it.stockItemId == "stock-1" }
        assertEquals("8", line.countedQty)
        assertEquals(VarianceReason.DAMAGE, line.reason)
    }

    @Test
    fun `an incomplete partial edit does not overwrite a previously-saved valid count`() = runTest {
        seedInProgressOpname()
        val firstVm = newViewModel()
        firstVm.onCountedQtyChange("stock-1", "15")
        // User starts retyping and briefly clears the field — this must not persist "systemQty"
        // over the previously-saved real count of 15.
        firstVm.onCountedQtyChange("stock-1", "")

        val savedLine = stockRepository.lines.values.first { it.stockItemId == "stock-1" }
        assertEquals(15.0, savedLine.countedQty, 0.0001)
    }
}
