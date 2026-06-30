package com.wfx.warungpos.fake

import com.wfx.warungpos.domain.model.Payment
import com.wfx.warungpos.domain.model.PaymentBreakdown
import com.wfx.warungpos.domain.model.PaymentMethod
import com.wfx.warungpos.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakePaymentRepository : PaymentRepository {
    val payments = mutableMapOf<String, Payment>()
    val methods = mutableMapOf<String, PaymentMethod>()
    var cashTotalForShift: Long = 0L
    var paymentBreakdown: List<PaymentBreakdown> = emptyList()

    override fun observePaymentsForBill(billId: String): Flow<List<Payment>> =
        flowOf(payments.values.filter { it.billId == billId })

    override fun observeActivePaymentMethods(): Flow<List<PaymentMethod>> =
        flowOf(methods.values.filter { it.isActive })

    override fun observeAllPaymentMethods(): Flow<List<PaymentMethod>> = flowOf(methods.values.toList())

    override suspend fun recordPayment(payment: Payment) {
        payments[payment.id] = payment
    }

    override suspend fun savePaymentMethod(method: PaymentMethod) {
        methods[method.id] = method
    }

    override suspend fun getCashPaymentsTotalForShift(shiftId: String): Long = cashTotalForShift

    override suspend fun getPaymentBreakdownForShift(shiftId: String): List<PaymentBreakdown> = paymentBreakdown

    override suspend fun getPaymentBreakdownInRange(startEpoch: Long, endEpoch: Long): List<PaymentBreakdown> = paymentBreakdown
}
