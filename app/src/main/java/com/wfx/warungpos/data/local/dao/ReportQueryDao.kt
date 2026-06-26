package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.wfx.warungpos.data.local.dao.pojo.BestSellerRow
import com.wfx.warungpos.data.local.dao.pojo.VoidSummary

@Dao
interface ReportQueryDao {
    @Query("""
        SELECT menuItemId, MAX(nameSnapshot) AS nameSnapshot,
               SUM(quantity) AS totalQty, SUM(lineTotal) AS totalRevenue
        FROM order_items
        WHERE status != 'VOID'
          AND billId IN (
              SELECT id FROM bills
              WHERE status = 'PAID' AND paidAt BETWEEN :startEpoch AND :endEpoch
          )
        GROUP BY menuItemId
        ORDER BY totalQty DESC
        LIMIT :limit
    """)
    suspend fun getBestSellers(startEpoch: Long, endEpoch: Long, limit: Int): List<BestSellerRow>

    @Query("""
        SELECT COUNT(*) AS count, COALESCE(SUM(lineTotal), 0) AS totalValue
        FROM order_items
        WHERE status = 'VOID'
          AND billId IN (SELECT id FROM bills WHERE shiftId = :shiftId)
    """)
    suspend fun totalVoidsForShift(shiftId: String): VoidSummary

    @Query("""
        SELECT COUNT(*) AS count, COALESCE(SUM(lineTotal), 0) AS totalValue
        FROM order_items
        WHERE status = 'VOID'
          AND billId IN (
              SELECT id FROM bills WHERE paidAt BETWEEN :startEpoch AND :endEpoch
          )
    """)
    suspend fun totalVoidsInRange(startEpoch: Long, endEpoch: Long): VoidSummary

    @Query("""
        SELECT COALESCE(SUM(grandTotal), 0)
        FROM bills
        WHERE shiftId = :shiftId AND status = 'PAID'
    """)
    suspend fun totalRevenueForShift(shiftId: String): Long

    @Query("""
        SELECT COALESCE(SUM(grandTotal), 0)
        FROM bills
        WHERE status = 'PAID' AND paidAt BETWEEN :startEpoch AND :endEpoch
    """)
    suspend fun totalRevenueInRange(startEpoch: Long, endEpoch: Long): Long

    @Query("""
        SELECT COUNT(*)
        FROM bills
        WHERE shiftId = :shiftId AND status = 'PAID'
    """)
    suspend fun totalTransactionsForShift(shiftId: String): Int
}
