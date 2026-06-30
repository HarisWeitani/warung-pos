package com.wfx.warungpos.fake

import com.wfx.warungpos.domain.model.BestSeller
import com.wfx.warungpos.domain.model.VoidStats
import com.wfx.warungpos.domain.repository.ReportRepository

class FakeReportRepository : ReportRepository {
    var revenueForShift: Long = 0L
    var transactionCountForShift: Int = 0
    var bestSellers: List<BestSeller> = emptyList()
    var voidStats: VoidStats = VoidStats(0, 0L)

    override suspend fun getBestSellers(startEpoch: Long, endEpoch: Long, limit: Int): List<BestSeller> = bestSellers

    override suspend fun getVoidStatsForShift(shiftId: String): VoidStats = voidStats

    override suspend fun getVoidStatsInRange(startEpoch: Long, endEpoch: Long): VoidStats = voidStats

    override suspend fun getTotalRevenueForShift(shiftId: String): Long = revenueForShift

    override suspend fun getTotalRevenueInRange(startEpoch: Long, endEpoch: Long): Long = revenueForShift

    override suspend fun getTransactionCountForShift(shiftId: String): Int = transactionCountForShift
}
