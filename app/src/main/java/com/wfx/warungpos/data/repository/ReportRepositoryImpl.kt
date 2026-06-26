package com.wfx.warungpos.data.repository

import com.wfx.warungpos.data.local.dao.ReportQueryDao
import com.wfx.warungpos.domain.model.BestSeller
import com.wfx.warungpos.domain.model.VoidStats
import com.wfx.warungpos.domain.repository.ReportRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val reportDao: ReportQueryDao,
) : ReportRepository {

    override suspend fun getBestSellers(startEpoch: Long, endEpoch: Long, limit: Int): List<BestSeller> =
        reportDao.getBestSellers(startEpoch, endEpoch, limit)
            .map { BestSeller(menuItemId = it.menuItemId, name = it.nameSnapshot, totalQty = it.totalQty, totalRevenue = it.totalRevenue) }

    override suspend fun getVoidStatsForShift(shiftId: String): VoidStats {
        val pojo = reportDao.totalVoidsForShift(shiftId)
        return VoidStats(count = pojo.count, totalValue = pojo.totalValue)
    }

    override suspend fun getVoidStatsInRange(startEpoch: Long, endEpoch: Long): VoidStats {
        val pojo = reportDao.totalVoidsInRange(startEpoch, endEpoch)
        return VoidStats(count = pojo.count, totalValue = pojo.totalValue)
    }

    override suspend fun getTotalRevenueForShift(shiftId: String): Long =
        reportDao.totalRevenueForShift(shiftId)

    override suspend fun getTotalRevenueInRange(startEpoch: Long, endEpoch: Long): Long =
        reportDao.totalRevenueInRange(startEpoch, endEpoch)

    override suspend fun getTransactionCountForShift(shiftId: String): Int =
        reportDao.totalTransactionsForShift(shiftId)
}
