package com.wfx.warungpos.data.seeding

import android.content.Context
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.data.local.dao.PaymentMethodDao
import com.wfx.warungpos.data.local.entity.PaymentMethodEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "warung_first_run"
private const val KEY_SEEDED = "seeded_v1"

// Fixed IDs so seeding is idempotent across reinstalls and test runs
// (id, name, sortOrder, isCash)
private val DEFAULT_PAYMENT_METHODS = listOf(
    Triple("pm_tunai", Pair("Tunai", true), 1),
    Triple("pm_qris", Pair("QRIS", false), 2),
    Triple("pm_gopay", Pair("GoPay", false), 3),
    Triple("pm_ovo", Pair("OVO", false), 4),
    Triple("pm_transfer", Pair("Transfer Bank", false), 5),
)

@Singleton
class FirstRunManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val paymentMethodDao: PaymentMethodDao,
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun ensureSeeded() {
        if (prefs.getBoolean(KEY_SEEDED, false)) return
        seedPaymentMethods()
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    private suspend fun seedPaymentMethods() {
        val now = System.currentTimeMillis()
        val entities = DEFAULT_PAYMENT_METHODS.map { (id, meta, order) ->
            val (name, isCash) = meta
            PaymentMethodEntity(
                id = id,
                name = name,
                isActive = true,
                isCash = isCash,
                sortOrder = order,
                updatedAt = now,
                syncStatus = SyncStatus.SYNCED.name,
                deviceId = "",
            )
        }
        paymentMethodDao.insertDefaults(entities)
    }
}
