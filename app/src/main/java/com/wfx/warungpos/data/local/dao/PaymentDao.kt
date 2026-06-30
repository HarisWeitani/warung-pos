package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.dao.pojo.PaymentSumByMethod
import com.wfx.warungpos.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Upsert
    suspend fun upsert(entity: PaymentEntity)

    @Query("SELECT * FROM payments WHERE billId = :billId")
    fun observeForBill(billId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE billId = :billId")
    suspend fun getForBill(billId: String): List<PaymentEntity>

    @Query("""
        SELECT paymentMethodId, SUM(amount) AS total
        FROM payments
        WHERE billId IN (
            SELECT id FROM bills WHERE shiftId = :shiftId AND status = 'PAID'
        )
        GROUP BY paymentMethodId
    """)
    suspend fun sumByMethodForShift(shiftId: String): List<PaymentSumByMethod>

    @Query("""
        SELECT paymentMethodId, SUM(amount) AS total
        FROM payments
        WHERE paidAt BETWEEN :startEpoch AND :endEpoch
        GROUP BY paymentMethodId
    """)
    suspend fun sumByMethodInRange(startEpoch: Long, endEpoch: Long): List<PaymentSumByMethod>

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM payments p
        WHERE p.billId IN (
            SELECT id FROM bills WHERE shiftId = :shiftId AND status = 'PAID'
        )
        AND p.paymentMethodId IN (
            SELECT id FROM payment_methods WHERE isCash = 1
        )
    """)
    suspend fun getCashTotalForShift(shiftId: String): Long

    @Query("SELECT * FROM payments WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<PaymentEntity>
}
