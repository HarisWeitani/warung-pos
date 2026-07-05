package com.wfx.warungpos.data.remote.sync

import android.database.sqlite.SQLiteConstraintException
import com.wfx.warungpos.data.local.db.WarungDatabase
import com.wfx.warungpos.data.remote.firebase.ChildEvent
import com.wfx.warungpos.data.remote.firebase.FirebaseRtdbDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_FK_RETRIES = 6
private const val FK_RETRY_BASE_DELAY_MS = 150L

@Singleton
class RtdbListener @Inject constructor(
    private val rtdb: FirebaseRtdbDataSource,
    private val db: WarungDatabase,
    private val conflictResolver: ConflictResolver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        listenPath(RtdbPaths.MENU_CATEGORIES) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toMenuCategoryEntity() ?: return@listenPath
                    val existing = db.menuCategoryDao().getById(incoming.id)
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = existing?.updatedAt,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.menuCategoryDao().upsert(incoming)
                    }
                }
                is ChildEvent.Removed -> db.menuCategoryDao().deleteById(event.snapshot.key ?: return@listenPath)
            }
        }

        listenPath(RtdbPaths.MENU_ITEMS) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toMenuItemEntity() ?: return@listenPath
                    val existing = db.menuItemDao().getById(incoming.id)
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = existing?.updatedAt,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.menuItemDao().upsert(incoming)
                    }
                }
                is ChildEvent.Removed -> db.menuItemDao().deleteById(event.snapshot.key ?: return@listenPath)
            }
        }

        listenPath(RtdbPaths.VARIANT_GROUPS) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toVariantGroupEntity() ?: return@listenPath
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = null,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.variantDao().upsertGroup(incoming)
                    }
                }
                is ChildEvent.Removed -> db.variantDao().deleteGroup(event.snapshot.key ?: return@listenPath)
            }
        }

        listenPath(RtdbPaths.VARIANT_OPTIONS) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toVariantOptionEntity() ?: return@listenPath
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = null,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.variantDao().upsertOption(incoming)
                    }
                }
                is ChildEvent.Removed -> db.variantDao().deleteOption(event.snapshot.key ?: return@listenPath)
            }
        }

        listenPath(RtdbPaths.TABLES) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toTableEntity() ?: return@listenPath
                    val existing = db.tableDao().getById(incoming.id)
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = existing?.updatedAt,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.tableDao().upsert(incoming)
                    }
                }
                is ChildEvent.Removed -> db.tableDao().deleteById(event.snapshot.key ?: return@listenPath)
            }
        }

        listenPath(RtdbPaths.SHIFTS) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toShiftEntity() ?: return@listenPath
                    val existing = db.shiftDao().getById(incoming.id)
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = existing?.updatedAt,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.shiftDao().upsert(incoming)
                    }
                }
                is ChildEvent.Removed -> Unit // shifts are never deleted
            }
        }

        listenPath(RtdbPaths.BILLS) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toBillEntity() ?: return@listenPath
                    val existing = db.billDao().getById(incoming.id)
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = existing?.updatedAt,
                        existingStatus = existing?.status,
                        incomingStatus = incoming.status,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.billDao().upsert(incoming)
                    }
                }
                is ChildEvent.Removed -> Unit // bills are soft-deleted via status
            }
        }

        listenPath(RtdbPaths.ORDER_ITEMS) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toOrderItemEntity() ?: return@listenPath
                    val existing = db.orderItemDao().getById(incoming.id)
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = existing?.updatedAt,
                        existingStatus = existing?.status,
                        incomingStatus = incoming.status,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.orderItemDao().upsert(incoming)
                    }
                }
                is ChildEvent.Removed -> Unit
            }
        }

        listenPath(RtdbPaths.PAYMENT_METHODS) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toPaymentMethodEntity() ?: return@listenPath
                    val existing = db.paymentMethodDao().getById(incoming.id)
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = existing?.updatedAt,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.paymentMethodDao().upsert(incoming)
                    }
                }
                is ChildEvent.Removed -> Unit
            }
        }

        listenPath(RtdbPaths.PAYMENTS) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toPaymentEntity() ?: return@listenPath
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = null,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.paymentDao().upsert(incoming)
                    }
                }
                is ChildEvent.Removed -> Unit
            }
        }

        listenPath(RtdbPaths.EXPENSES) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toExpenseEntity() ?: return@listenPath
                    val existing = db.expenseDao().getById(incoming.id)
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = existing?.updatedAt,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.expenseDao().upsert(incoming)
                    }
                }
                is ChildEvent.Removed -> Unit
            }
        }

        listenPath(RtdbPaths.STOCK_ITEMS) { event ->
            when (event) {
                is ChildEvent.Added, is ChildEvent.Changed -> {
                    val incoming = event.snapshot.toStockItemEntity() ?: return@listenPath
                    val existing = db.stockDao().getItemById(incoming.id)
                    val resolution = conflictResolver.resolve(
                        incomingUpdatedAt = incoming.updatedAt,
                        existingUpdatedAt = existing?.updatedAt,
                    )
                    if (resolution == ConflictResolution.ACCEPT) {
                        db.stockDao().upsertItem(incoming)
                    }
                }
                is ChildEvent.Removed -> Unit
            }
        }
    }

    private fun listenPath(path: String, handler: suspend (ChildEvent) -> Unit) {
        rtdb.observeChildren(path)
            .onEach { event -> scope.launch { applyResiliently(event, handler) } }
            .catch { /* log in production — silently ignored during Phase 2 */ }
            .launchIn(scope)
    }

    /**
     * Applies one remote change, tolerating out-of-order arrival across the independent per-path
     * listeners. A child (e.g. a bill) can be delivered before its parent (its shift/table) has
     * been synced by that parent's own listener, which fails the Room FOREIGN KEY check. Rather
     * than crash the sync thread, we retry a few times with a short backoff — by then the parent
     * has almost always been inserted. If it genuinely never arrives (a true orphan), we give up
     * quietly instead of taking down the app. Any other exception is swallowed for the same reason.
     */
    private suspend fun applyResiliently(event: ChildEvent, handler: suspend (ChildEvent) -> Unit) {
        var attempt = 0
        while (true) {
            try {
                handler(event)
                return
            } catch (e: SQLiteConstraintException) {
                attempt++
                if (attempt >= MAX_FK_RETRIES) return
                delay(FK_RETRY_BASE_DELAY_MS * attempt)
            } catch (e: Exception) {
                return
            }
        }
    }
}
