package com.wfx.warungpos.domain.repository

import com.wfx.warungpos.domain.model.BestSeller
import com.wfx.warungpos.domain.model.VoidStats

interface ReportRepository {
    suspend fun getBestSellers(startEpoch: Long, endEpoch: Long, limit: Int): List<BestSeller>
    suspend fun getVoidStatsForShift(shiftId: String): VoidStats
    suspend fun getVoidStatsInRange(startEpoch: Long, endEpoch: Long): VoidStats
    suspend fun getTotalRevenueForShift(shiftId: String): Long
    suspend fun getTotalRevenueInRange(startEpoch: Long, endEpoch: Long): Long
    suspend fun getTransactionCountForShift(shiftId: String): Int
}
