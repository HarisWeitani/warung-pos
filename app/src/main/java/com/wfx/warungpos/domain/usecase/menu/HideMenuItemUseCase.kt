package com.wfx.warungpos.domain.usecase.menu

import com.wfx.warungpos.domain.repository.MenuRepository
import javax.inject.Inject

class HideMenuItemUseCase @Inject constructor(private val menuRepository: MenuRepository) {
    suspend operator fun invoke(itemId: String): Result<Unit> {
        val item = menuRepository.getMenuItem(itemId)
            ?: return Result.failure(IllegalArgumentException("Item not found"))
        menuRepository.saveMenuItem(item.copy(isAvailable = false))
        return Result.success(Unit)
    }
}
