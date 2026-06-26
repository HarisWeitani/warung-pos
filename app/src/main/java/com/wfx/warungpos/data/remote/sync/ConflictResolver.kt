package com.wfx.warungpos.data.remote.sync

import javax.inject.Inject
import javax.inject.Singleton

enum class ConflictResolution { ACCEPT, REJECT }

@Singleton
class ConflictResolver @Inject constructor() {

    fun resolve(
        incomingUpdatedAt: Long?,
        existingUpdatedAt: Long?,
        existingStatus: String? = null,
        incomingStatus: String? = null,
    ): ConflictResolution {
        // No existing record — always accept
        if (existingUpdatedAt == null) return ConflictResolution.ACCEPT

        // Bill/order status is forward-only: OPEN → PAID → VOID
        if (existingStatus != null && incomingStatus != null) {
            if (isStatusRegression(existingStatus, incomingStatus)) {
                return ConflictResolution.REJECT
            }
        }

        // Last-write-wins by updatedAt
        if (incomingUpdatedAt == null) return ConflictResolution.REJECT
        return if (incomingUpdatedAt > existingUpdatedAt) ConflictResolution.ACCEPT
        else ConflictResolution.REJECT
    }

    private fun isStatusRegression(existing: String, incoming: String): Boolean {
        val order = listOf("OPEN", "PAID", "VOID")
        val existingIdx = order.indexOf(existing)
        val incomingIdx = order.indexOf(incoming)
        return existingIdx >= 0 && incomingIdx in 0 until existingIdx
    }
}
