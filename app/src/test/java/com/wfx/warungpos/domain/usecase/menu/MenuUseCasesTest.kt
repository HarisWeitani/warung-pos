package com.wfx.warungpos.domain.usecase.menu

import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.fake.FakeMenuRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpsertMenuItemUseCaseTest {

    private fun item(name: String = "Nasi Goreng", price: Long = 15_000L, categoryId: String? = "cat-1") = MenuItem(
        id = "item-1", categoryId = categoryId, name = name, basePrice = price,
        isAvailable = true, isSoldOut = false, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Test
    fun blankName_failsWithIllegalArgumentException() = runTest {
        val repo = FakeMenuRepository()
        val result = UpsertMenuItemUseCase(repo)(item(name = "  "))
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun zeroPrice_failsWithIllegalArgumentException() = runTest {
        val repo = FakeMenuRepository()
        val result = UpsertMenuItemUseCase(repo)(item(price = 0L))
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun nullCategory_failsWithIllegalArgumentException() = runTest {
        val repo = FakeMenuRepository()
        val result = UpsertMenuItemUseCase(repo)(item(categoryId = null))
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun validItem_savesSuccessfully() = runTest {
        val repo = FakeMenuRepository()
        val result = UpsertMenuItemUseCase(repo)(item())
        assertTrue(result.isSuccess)
        assertEquals("Nasi Goreng", repo.items["item-1"]!!.name)
    }
}

class ToggleSoldOutUseCaseTest {

    private fun item(soldOut: Boolean) = MenuItem(
        id = "item-1", categoryId = "cat-1", name = "Item", basePrice = 10_000L,
        isAvailable = true, isSoldOut = soldOut, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Test
    fun togglesSoldOutFlagOnItem() = runTest {
        val repo = FakeMenuRepository()
        repo.items["item-1"] = item(soldOut = false)
        ToggleSoldOutUseCase(repo)("item-1", true)
        assertTrue(repo.items["item-1"]!!.isSoldOut)
    }

    @Test
    fun togglesBackToAvailable() = runTest {
        val repo = FakeMenuRepository()
        repo.items["item-1"] = item(soldOut = true)
        ToggleSoldOutUseCase(repo)("item-1", false)
        assertFalse(repo.items["item-1"]!!.isSoldOut)
    }
}

class HideMenuItemUseCaseTest {

    private fun item() = MenuItem(
        id = "item-1", categoryId = "cat-1", name = "Item", basePrice = 10_000L,
        isAvailable = true, isSoldOut = false, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Test
    fun itemNotFound_failsWithIllegalArgumentException() = runTest {
        val repo = FakeMenuRepository()
        val result = HideMenuItemUseCase(repo)("nonexistent")
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun existingItem_setsIsAvailableFalse() = runTest {
        val repo = FakeMenuRepository()
        repo.items["item-1"] = item()
        val result = HideMenuItemUseCase(repo)("item-1")
        assertTrue(result.isSuccess)
        assertFalse(repo.items["item-1"]!!.isAvailable)
    }
}
