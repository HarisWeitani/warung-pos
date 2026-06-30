package com.wfx.warungpos.domain.usecase.shift

import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.fake.FakeMenuRepository
import com.wfx.warungpos.fake.FakeSessionProvider
import com.wfx.warungpos.fake.FakeShiftRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenShiftUseCaseTest {

    private lateinit var shiftRepository: FakeShiftRepository
    private lateinit var sessionProvider: FakeSessionProvider
    private lateinit var useCase: OpenShiftUseCase

    @Before
    fun setup() {
        shiftRepository = FakeShiftRepository()
        sessionProvider = FakeSessionProvider()
        useCase = OpenShiftUseCase(shiftRepository, sessionProvider)
    }

    @Test
    fun `open shift when no existing shift succeeds`() = runTest {
        val result = useCase(100_000L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `open shift when shift already open fails with IllegalStateException`() = runTest {
        useCase(100_000L)
        val result = useCase(50_000L)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}

class CheckSoldOutItemsUseCaseTest {

    private fun menuItem(id: String, soldOut: Boolean) = MenuItem(
        id = id, categoryId = "cat-1", name = "Item", basePrice = 10_000L,
        isAvailable = true, isSoldOut = soldOut, updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Test
    fun `returns true when one item has isSoldOut true`() = runTest {
        val menuRepository = FakeMenuRepository()
        menuRepository.items["item-1"] = menuItem("item-1", soldOut = true)
        val useCase = CheckSoldOutItemsUseCase(menuRepository)
        assertTrue(useCase())
    }

    @Test
    fun `returns false when zero sold-out items`() = runTest {
        val menuRepository = FakeMenuRepository()
        menuRepository.items["item-1"] = menuItem("item-1", soldOut = false)
        val useCase = CheckSoldOutItemsUseCase(menuRepository)
        assertFalse(useCase())
    }
}

class ResetSoldOutItemsUseCaseTest {

    private fun menuItem(id: String, soldOut: Boolean) = MenuItem(
        id = id, categoryId = "cat-1", name = "Item", basePrice = 10_000L,
        isAvailable = true, isSoldOut = soldOut, updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Test
    fun `resets all sold-out items back to available`() = runTest {
        val menuRepository = FakeMenuRepository()
        menuRepository.items["item-1"] = menuItem("item-1", soldOut = true)
        menuRepository.items["item-2"] = menuItem("item-2", soldOut = true)
        menuRepository.items["item-3"] = menuItem("item-3", soldOut = false)

        val useCase = ResetSoldOutItemsUseCase(menuRepository)
        useCase()

        assertTrue(menuRepository.items.values.none { it.isSoldOut })
    }
}
