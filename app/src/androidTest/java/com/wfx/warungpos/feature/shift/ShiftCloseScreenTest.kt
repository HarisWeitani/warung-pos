package com.wfx.warungpos.feature.shift

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.ui.theme.WarungPosTheme
import org.junit.Rule
import org.junit.Test

/**
 * Covers TICKET-097 (Shift Close Blocked by Open Bill) at the screen level: when openBills is
 * non-empty the blocking list is shown and Close Shift is disabled; once the ViewModel's state
 * reflects zero open bills (after the bill is paid elsewhere), the button re-enables. Driven
 * directly via ShiftCloseViewModel.UiState rather than the full Hilt/Navigation/Firebase-backed
 * flow — see the PaymentScreenTest doc comment for the rationale.
 */
class ShiftCloseScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val openShift = Shift(
        id = "shift-1", openedBy = "user-1", closedBy = null, status = ShiftStatus.OPEN,
        openedAt = 0L, closedAt = null, openingFloat = 100_000L, closingFloat = null,
        updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    private val openBill = Bill(
        id = "bill-1", status = BillStatus.OPEN,
        sessionLabel = "Meja 1", createdAt = 0L, paidAt = null, subtotal = 25_000L,
        discountTotal = 0L, grandTotal = 25_000L, note = null, shiftId = "shift-1",
        voidReason = null, voidedBy = null, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Test
    fun openBillsPresent_blockingListShownAndCloseShiftDisabled() {
        val state = ShiftCloseViewModel.UiState(shift = openShift, openBills = listOf(openBill))
        composeTestRule.setContent {
            WarungPosTheme {
                ShiftCloseScreen(state = state, onFloatChange = {}, onCloseShift = {}, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Cannot close: 1 open bill(s)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Meja 1", substring = true).assertIsDisplayed()
        composeTestRule.onNode(hasText("Close Day") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun noOpenBills_closeShiftEnabled() {
        val state = ShiftCloseViewModel.UiState(shift = openShift, openBills = emptyList())
        composeTestRule.setContent {
            WarungPosTheme {
                ShiftCloseScreen(state = state, onFloatChange = {}, onCloseShift = {}, onBack = {})
            }
        }
        composeTestRule.onNode(hasText("Close Day") and hasClickAction()).assertIsEnabled()
    }

    // DEFECT-016 regression coverage: an OPEN shift other than the current one — as left behind
    // by another device — must be visible and actionable from this screen, not silently absent.
    private val orphanShift = Shift(
        id = "shift-orphan", openedBy = "user-2", closedBy = null, status = ShiftStatus.OPEN,
        openedAt = 0L, closedAt = null, openingFloat = 0L, closingFloat = null,
        updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev-2",
    )

    @Test
    fun defect016Regression_otherOpenShiftWithNoBillsShowsCloseButton() {
        val state = ShiftCloseViewModel.UiState(
            shift = openShift,
            otherOpenShifts = listOf(ShiftCloseViewModel.OtherOpenShift(shift = orphanShift, openBillCount = 0)),
        )
        composeTestRule.setContent {
            WarungPosTheme {
                ShiftCloseScreen(state = state, onFloatChange = {}, onCloseShift = {}, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Other Open Shifts Detected (1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close This Shift").assertIsDisplayed()
    }

    @Test
    fun defect016Regression_otherOpenShiftWithBillsIsBlockedNotClosable() {
        val state = ShiftCloseViewModel.UiState(
            shift = openShift,
            otherOpenShifts = listOf(ShiftCloseViewModel.OtherOpenShift(shift = orphanShift, openBillCount = 1)),
        )
        composeTestRule.setContent {
            WarungPosTheme {
                ShiftCloseScreen(state = state, onFloatChange = {}, onCloseShift = {}, onBack = {})
            }
        }
        composeTestRule.onNodeWithText("1 open bill(s) must be resolved first — check Orders").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Close This Shift").assertCountEquals(0)
    }

    @Test
    fun noOtherOpenShifts_sectionIsAbsent() {
        val state = ShiftCloseViewModel.UiState(shift = openShift, otherOpenShifts = emptyList())
        composeTestRule.setContent {
            WarungPosTheme {
                ShiftCloseScreen(state = state, onFloatChange = {}, onCloseShift = {}, onBack = {})
            }
        }
        composeTestRule.onAllNodesWithText("Other Open Shifts Detected", substring = true).assertCountEquals(0)
    }

    @Test
    fun defect016Regression_closingOtherShiftInvokesCallbackWithCorrectShiftId() {
        var closedShiftId: String? = null
        val state = ShiftCloseViewModel.UiState(
            shift = openShift,
            otherOpenShifts = listOf(ShiftCloseViewModel.OtherOpenShift(shift = orphanShift, openBillCount = 0)),
        )
        composeTestRule.setContent {
            WarungPosTheme {
                ShiftCloseScreen(
                    state = state, onFloatChange = {}, onCloseShift = {}, onBack = {},
                    onCloseOtherShift = { closedShiftId = it },
                )
            }
        }
        composeTestRule.onNodeWithText("Close This Shift").performClick()
        assert(closedShiftId == orphanShift.id)
    }
}
