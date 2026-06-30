package com.wfx.warungpos.domain.usecase.shift

import com.wfx.warungpos.domain.repository.MenuRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ResetSoldOutItemsUseCase @Inject constructor(private val menuRepository: MenuRepository) {
    suspend operator fun invoke() {
        menuRepository.observeAllItems().first()
            .filter { it.isSoldOut }
            .forEach { menuRepository.updateSoldOut(it.id, false) }
    }
}
