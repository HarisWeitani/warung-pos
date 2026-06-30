package com.wfx.warungpos.feature.payment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.BillType
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.PaymentMethod
import com.wfx.warungpos.ui.theme.WarungPosTheme
import org.junit.Rule
import org.junit.Test

/**
 * Covers the payment portion of the Grab & Go flow described in TICKET-096: select a method,
 * enter a cash tender greater than the total, see the change reflected, and confirm Pay is only
 * enabled once a method is selected. This drives the PaymentScreen composable directly with a
 * fixed PaymentUiState rather than through Hilt/Navigation/Firebase — see TICKET-096 note in the
 * Phase 5 commit history for why the full DI-backed E2E flow isn't exercised here.
 */
class PaymentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val bill = Bill(
        id = "bill-1", tableId = null, type = BillType.UPFRONT, status = BillStatus.OPEN,
        sessionLabel = "Counter", createdAt = 0L, paidAt = null, subtotal = 45_000L,
        discountTotal = 0L, grandTotal = 45_000L, note = null, shiftId = "shift-1",
        voidReason = null, voidedBy = null, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    private val cashMethod = PaymentMethod(
        id = "pm_tunai", name = "Tunai", isActive = true, isCash = true,
        sortOrder = 1, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Test
    fun payButton_disabledUntilMethodSelected() {
        val state = PaymentUiState(bill = bill, paymentMethods = listOf(cashMethod), selectedMethodId = null)
        composeTestRule.setContent {
            WarungPosTheme {
                PaymentScreen(state = state, onBack = {}, onSelectMethod = {}, onTenderChange = {}, onConfirm = {})
            }
        }
        composeTestRule.onNodeWithText("Confirm Payment").assertIsNotEnabled()
    }

    @Test
    fun overpaidCashTender_showsCorrectChange() {
        val state = PaymentUiState(
            bill = bill,
            paymentMethods = listOf(cashMethod),
            selectedMethodId = cashMethod.id,
            tenderAmount = "50000",
            change = 5_000L,
        )
        composeTestRule.setContent {
            WarungPosTheme {
                PaymentScreen(state = state, onBack = {}, onSelectMethod = {}, onTenderChange = {}, onConfirm = {})
            }
        }
        composeTestRule.onNodeWithText("Rp 5.000", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Payment").assertIsEnabled()
    }

    @Test
    fun tappingPay_invokesOnConfirm() {
        var confirmed = false
        val state = PaymentUiState(
            bill = bill, paymentMethods = listOf(cashMethod), selectedMethodId = cashMethod.id,
            tenderAmount = "45000",
        )
        composeTestRule.setContent {
            WarungPosTheme {
                PaymentScreen(state = state, onBack = {}, onSelectMethod = {}, onTenderChange = {}, onConfirm = { confirmed = true })
            }
        }
        composeTestRule.onNodeWithText("Confirm Payment").performClick()
        assert(confirmed)
    }

    @Test
    fun successState_showsPaymentCompleteMessage() {
        val state = PaymentUiState(bill = bill, paymentMethods = listOf(cashMethod), isSuccess = true)
        composeTestRule.setContent {
            WarungPosTheme {
                PaymentScreen(state = state, onBack = {}, onSelectMethod = {}, onTenderChange = {}, onConfirm = {})
            }
        }
        composeTestRule.onNodeWithText("Payment Complete!", substring = true).assertIsDisplayed()
    }
}
