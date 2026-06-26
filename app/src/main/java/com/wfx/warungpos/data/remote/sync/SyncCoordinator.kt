package com.wfx.warungpos.data.remote.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.wfx.warungpos.core.common.NetworkMonitor
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val SYNC_WORK_NAME = "warung_pos_sync"

@Singleton
class SyncCoordinator @Inject constructor(
    private val workManager: WorkManager,
    private val rtdbListener: RtdbListener,
    val networkMonitor: NetworkMonitor,
) {
    val isOnline: StateFlow<Boolean> get() = networkMonitor.isOnline

    fun start() {
        rtdbListener.start()
    }

    fun notifyPendingSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }
}
