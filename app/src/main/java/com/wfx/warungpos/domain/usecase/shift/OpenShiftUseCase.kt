package com.wfx.warungpos.domain.usecase.shift

import com.wfx.warungpos.core.common.SessionProvider
import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.repository.ShiftRepository
import javax.inject.Inject

class OpenShiftUseCase @Inject constructor(
    private val shiftRepository: ShiftRepository,
    private val sessionProvider: SessionProvider,
) {
    suspend operator fun invoke(openingFloat: Long): Result<String> {
        if (shiftRepository.getOpenShift() != null) {
            return Result.failure(IllegalStateException("A shift is already open"))
        }
        val now = DateUtil.nowEpochMs()
        val shift = Shift(
            id = UuidGenerator.generate(),
            openedBy = sessionProvider.currentUserId ?: "",
            closedBy = null,
            status = ShiftStatus.OPEN,
            openedAt = now,
            closedAt = null,
            openingFloat = openingFloat,
            closingFloat = null,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
            deviceId = sessionProvider.deviceId,
        )
        shiftRepository.saveShift(shift)
        return Result.success(shift.id)
    }
}
