package com.wfx.warungpos.data.remote.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.wfx.warungpos.core.common.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

    val isSyncing: Flow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(SYNC_WORK_NAME)
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }

    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
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
