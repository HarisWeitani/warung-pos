package com.wfx.warungpos.domain.usecase.payment

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.SessionProvider
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.exception.BillAlreadyPaidException
import com.wfx.warungpos.domain.exception.InsufficientPaymentException
import com.wfx.warungpos.domain.exception.InsufficientTenderedAmountException
import com.wfx.warungpos.domain.exception.ShiftNotOpenException
import com.wfx.warungpos.domain.model.Payment
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.PaymentRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import com.wfx.warungpos.domain.usecase.stock.DeductStockForBillUseCase
import javax.inject.Inject

class ProcessPaymentUseCase @Inject constructor(
    private val billRepository: BillRepository,
    private val paymentRepository: PaymentRepository,
    private val shiftRepository: ShiftRepository,
    private val sessionProvider: SessionProvider,
    private val deductStockForBillUseCase: DeductStockForBillUseCase,
) {
    suspend operator fun invoke(billId: String, rows: List<PaymentRow>): Result<Unit> {
        shiftRepository.getOpenShift() ?: return Result.failure(ShiftNotOpenException())

        val bill = billRepository.getBill(billId)
            ?: return Result.failure(IllegalArgumentException("Bill not found"))
        if (bill.status == BillStatus.PAID) return Result.failure(BillAlreadyPaidException())

        for (row in rows) {
            if (row.tenderedAmount < row.amount) {
                return Result.failure(InsufficientTenderedAmountException())
            }
        }

        val totalPaid = rows.sumOf { it.amount }
        if (totalPaid < bill.grandTotal) {
            return Result.failure(InsufficientPaymentException())
        }

        val now = DateUtil.nowEpochMs()
        val deviceId = sessionProvider.deviceId
        val payments = rows.map { row ->
            Payment(
                id = UuidGenerator.generate(),
                billId = billId,
                paymentMethodId = row.methodId,
                amount = row.amount,
                change = maxOf(0L, row.tenderedAmount - row.amount),
                paidAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING,
                deviceId = deviceId,
            )
        }
        paymentRepository.processPaymentTransaction(payments, bill.copy(status = BillStatus.PAID, paidAt = now))
        deductStockForBillUseCase(billId)
        return Result.success(Unit)
    }
}
