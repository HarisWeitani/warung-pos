package com.wfx.warungpos.data.remote.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.data.local.db.WarungDatabase
import com.wfx.warungpos.data.remote.firebase.FirebaseRtdbDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val rtdb: FirebaseRtdbDataSource,
    private val db: WarungDatabase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val updates = mutableMapOf<String, Any?>()

        db.menuCategoryDao().getPendingSync().forEach { entity ->
            updates["/${RtdbPaths.MENU_CATEGORIES}/${entity.id}"] = entity.toRtdbMap()
        }
        db.menuItemDao().getPendingSync().forEach { entity ->
            updates["/${RtdbPaths.MENU_ITEMS}/${entity.id}"] = entity.toRtdbMap()
        }
        db.variantDao().getPendingGroups().forEach { entity ->
            updates["/${RtdbPaths.VARIANT_GROUPS}/${entity.id}"] = entity.toRtdbMap()
        }
        db.variantDao().getPendingOptions().forEach { entity ->
            updates["/${RtdbPaths.VARIANT_OPTIONS}/${entity.id}"] = entity.toRtdbMap()
        }
        db.tableDao().getPendingSync().forEach { entity ->
            updates["/${RtdbPaths.TABLES}/${entity.id}"] = entity.toRtdbMap()
        }
        db.shiftDao().getPendingSync().forEach { entity ->
            updates["/${RtdbPaths.SHIFTS}/${entity.id}"] = entity.toRtdbMap()
        }
        db.billDao().getPendingSync().forEach { entity ->
            updates["/${RtdbPaths.BILLS}/${entity.id}"] = entity.toRtdbMap()
        }
        db.orderItemDao().getPendingSync().forEach { entity ->
            updates["/${RtdbPaths.ORDER_ITEMS}/${entity.id}"] = entity.toRtdbMap()
        }
        db.paymentMethodDao().getPendingSync().forEach { entity ->
            updates["/${RtdbPaths.PAYMENT_METHODS}/${entity.id}"] = entity.toRtdbMap()
        }
        db.paymentDao().getPendingSync().forEach { entity ->
            updates["/${RtdbPaths.PAYMENTS}/${entity.id}"] = entity.toRtdbMap()
        }
        db.expenseDao().getPendingSync().forEach { entity ->
            updates["/${RtdbPaths.EXPENSES}/${entity.id}"] = entity.toRtdbMap()
        }
        db.stockDao().getPendingItems().forEach { entity ->
            updates["/${RtdbPaths.STOCK_ITEMS}/${entity.id}"] = entity.toRtdbMap()
        }

        if (updates.isNotEmpty()) {
            rtdb.writeMulti(updates)
            markSynced()
        }
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { if (runAttemptCount < 3) Result.retry() else Result.failure() },
    )

    private suspend fun markSynced() {
        val synced = SyncStatus.SYNCED.name
        val pending = SyncStatus.PENDING.name
        // Re-check each DAO and flip PENDING → SYNCED
        db.menuCategoryDao().getPendingSync().forEach { e ->
            db.menuCategoryDao().upsert(e.copy(syncStatus = synced))
        }
        db.menuItemDao().getPendingSync().forEach { e ->
            db.menuItemDao().upsert(e.copy(syncStatus = synced))
        }
        db.tableDao().getPendingSync().forEach { e ->
            db.tableDao().upsert(e.copy(syncStatus = synced))
        }
        db.shiftDao().getPendingSync().forEach { e ->
            db.shiftDao().upsert(e.copy(syncStatus = synced))
        }
        db.billDao().getPendingSync().forEach { e ->
            db.billDao().upsert(e.copy(syncStatus = synced))
        }
        db.orderItemDao().getPendingSync().forEach { e ->
            db.orderItemDao().upsert(e.copy(syncStatus = synced))
        }
        db.paymentMethodDao().getPendingSync().forEach { e ->
            db.paymentMethodDao().upsert(e.copy(syncStatus = synced))
        }
        db.paymentDao().getPendingSync().forEach { e ->
            db.paymentDao().upsert(e.copy(syncStatus = synced))
        }
        db.expenseDao().getPendingSync().forEach { e ->
            db.expenseDao().upsert(e.copy(syncStatus = synced))
        }
    }
}
