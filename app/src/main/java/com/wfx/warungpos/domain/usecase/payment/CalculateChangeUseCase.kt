package com.wfx.warungpos.domain.usecase.payment

import javax.inject.Inject

class CalculateChangeUseCase @Inject constructor() {
    operator fun invoke(tendered: Long, total: Long): Long = tendered - total
}
