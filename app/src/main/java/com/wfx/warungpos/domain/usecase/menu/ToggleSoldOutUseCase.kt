package com.wfx.warungpos.domain.usecase.menu

import com.wfx.warungpos.domain.repository.MenuRepository
import javax.inject.Inject

class ToggleSoldOutUseCase @Inject constructor(private val menuRepository: MenuRepository) {
    suspend operator fun invoke(itemId: String, soldOut: Boolean) =
        menuRepository.updateSoldOut(itemId, soldOut)
}
