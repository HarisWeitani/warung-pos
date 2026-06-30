package com.wfx.warungpos.domain.repository

import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.Payment
import com.wfx.warungpos.domain.model.PaymentBreakdown
import com.wfx.warungpos.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun observePaymentsForBill(billId: String): Flow<List<Payment>>
    fun observeActivePaymentMethods(): Flow<List<PaymentMethod>>
    fun observeAllPaymentMethods(): Flow<List<PaymentMethod>>
    suspend fun recordPayment(payment: Payment)

    /** Atomically writes all payment rows and the updated bill in a single transaction. */
    suspend fun processPaymentTransaction(payments: List<Payment>, updatedBill: Bill)
    suspend fun savePaymentMethod(method: PaymentMethod)
    suspend fun getCashPaymentsTotalForShift(shiftId: String): Long
    suspend fun getPaymentBreakdownForShift(shiftId: String): List<PaymentBreakdown>
    suspend fun getPaymentBreakdownInRange(startEpoch: Long, endEpoch: Long): List<PaymentBreakdown>
}
