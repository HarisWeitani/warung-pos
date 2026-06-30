package com.wfx.warungpos.domain.usecase.shift

import com.wfx.warungpos.domain.repository.MenuRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CheckSoldOutItemsUseCase @Inject constructor(private val menuRepository: MenuRepository) {
    suspend operator fun invoke(): Boolean =
        menuRepository.observeAllItems().first().any { it.isSoldOut }
}
