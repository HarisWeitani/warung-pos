package com.wfx.warungpos.domain.usecase.bill

import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.repository.BillRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOpenBillsUseCase @Inject constructor(private val billRepository: BillRepository) {
    operator fun invoke(): Flow<List<Bill>> = billRepository.observeOpenBills()
}
