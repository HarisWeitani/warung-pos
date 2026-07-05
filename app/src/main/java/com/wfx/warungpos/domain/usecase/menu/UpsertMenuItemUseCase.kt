package com.wfx.warungpos.domain.usecase.menu

import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.repository.MenuRepository
import javax.inject.Inject

class UpsertMenuItemUseCase @Inject constructor(private val menuRepository: MenuRepository) {
    suspend operator fun invoke(item: MenuItem): Result<Unit> {
        if (item.name.isBlank()) return Result.failure(IllegalArgumentException("Name must not be blank"))
        if (item.basePrice <= 0) return Result.failure(IllegalArgumentException("Price must be greater than 0"))
        menuRepository.saveMenuItem(item)
        return Result.success(Unit)
    }
}
