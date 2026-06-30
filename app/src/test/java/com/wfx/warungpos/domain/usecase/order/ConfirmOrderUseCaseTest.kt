package com.wfx.warungpos.domain.usecase.order

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.BillType
import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VariantSelectionType
import com.wfx.warungpos.domain.exception.BillAlreadyPaidException
import com.wfx.warungpos.domain.exception.EmptyCartException
import com.wfx.warungpos.domain.exception.MissingRequiredVariantException
import com.wfx.warungpos.domain.exception.ShiftNotOpenException
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.CartItem
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.model.OrderDestination
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.model.VariantGroup
import com.wfx.warungpos.domain.model.VariantOption
import com.wfx.warungpos.domain.model.VariantSelection
import com.wfx.warungpos.fake.FakeBillRepository
import com.wfx.warungpos.fake.FakeMenuRepository
import com.wfx.warungpos.fake.FakeOrderRepository
import com.wfx.warungpos.fake.FakeSessionProvider
import com.wfx.warungpos.fake.FakeShiftRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConfirmOrderUseCaseTest {

    private lateinit var billRepository: FakeBillRepository
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var menuRepository: FakeMenuRepository
    private lateinit var shiftRepository: FakeShiftRepository
    private lateinit var sessionProvider: FakeSessionProvider
    private lateinit var useCase: ConfirmOrderUseCase

    private val menuItem = MenuItem(
        id = "item-1",
        categoryId = "cat-1",
        name = "Nasi Goreng",
        basePrice = 15_000L,
        isAvailable = true,
        isSoldOut = false,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        deviceId = "dev",
    )

    @Before
    fun setup() {
        billRepository = FakeBillRepository()
        orderRepository = FakeOrderRepository()
        menuRepository = FakeMenuRepository()
        shiftRepository = FakeShiftRepository()
        sessionProvider = FakeSessionProvider()
        useCase = ConfirmOrderUseCase(billRepository, orderRepository, menuRepository, shiftRepository, sessionProvider)

        menuRepository.items[menuItem.id] = menuItem
        shiftRepository.shifts["shift-1"] = Shift(
            id = "shift-1",
            openedBy = "user-1",
            closedBy = null,
            status = ShiftStatus.OPEN,
            openedAt = 0L,
            closedAt = null,
            openingFloat = 100_000L,
            closingFloat = null,
            updatedAt = 0L,
            syncStatus = SyncStatus.SYNCED,
            deviceId = "dev",
        )
    }

    @Test
    fun `valid cart with active shift and new table succeeds`() = runTest {
        val cart = listOf(CartItem(menuItem, 2, emptyList()))
        val result = useCase(cart, OrderDestination.NewTable("table-1"))
        assertTrue(result.isSuccess)
    }

    @Test
    fun `empty cart fails with EmptyCartException`() = runTest {
        val result = useCase(emptyList(), OrderDestination.GrabAndGo)
        assertTrue(result.exceptionOrNull() is EmptyCartException)
    }

    @Test
    fun `no active shift fails with ShiftNotOpenException`() = runTest {
        shiftRepository.shifts.clear()
        val cart = listOf(CartItem(menuItem, 1, emptyList()))
        val result = useCase(cart, OrderDestination.GrabAndGo)
        assertTrue(result.exceptionOrNull() is ShiftNotOpenException)
    }

    @Test
    fun `required variant group unfulfilled fails with MissingRequiredVariantException`() = runTest {
        val group = VariantGroup(
            id = "group-1", menuItemId = menuItem.id, name = "Spice Level",
            selectionType = VariantSelectionType.SINGLE, isRequired = true,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
        )
        menuRepository.variantGroups[group.id] = group
        val cart = listOf(CartItem(menuItem, 1, emptyList()))
        val result = useCase(cart, OrderDestination.GrabAndGo)
        assertTrue(result.exceptionOrNull() is MissingRequiredVariantException)
    }

    @Test
    fun `grab-and-go destination creates bill with null tableId and UPFRONT type`() = runTest {
        val cart = listOf(CartItem(menuItem, 1, emptyList()))
        val result = useCase(cart, OrderDestination.GrabAndGo)
        val billId = result.getOrThrow()
        val bill = billRepository.getBill(billId)!!
        assertNull(bill.tableId)
        assertEquals(BillType.UPFRONT, bill.type)
    }

    @Test
    fun `existing bill destination with PAID bill fails with BillAlreadyPaidException`() = runTest {
        billRepository.bills["bill-1"] = Bill(
            id = "bill-1", tableId = null, type = BillType.UPFRONT, status = BillStatus.PAID,
            sessionLabel = "Counter", createdAt = 0L, paidAt = 0L, subtotal = 0L, discountTotal = 0L,
            grandTotal = 0L, note = null, shiftId = "shift-1", voidReason = null, voidedBy = null,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
        )
        val cart = listOf(CartItem(menuItem, 1, emptyList()))
        val result = useCase(cart, OrderDestination.ExistingBill("bill-1"))
        assertTrue(result.exceptionOrNull() is BillAlreadyPaidException)
    }

    @Test
    fun `price snapshot equals basePrice plus sum of selected option price deltas`() = runTest {
        val variants = listOf(
            VariantSelection(groupId = "g1", groupName = "Size", optionId = "o1", optionName = "Large", priceDelta = 3_000L),
            VariantSelection(groupId = "g2", groupName = "Spice", optionId = "o2", optionName = "Extra Hot", priceDelta = 2_000L),
        )
        val cart = listOf(CartItem(menuItem, 1, variants))
        val billId = useCase(cart, OrderDestination.GrabAndGo).getOrThrow()
        val item = orderRepository.items.values.first { it.billId == billId }
        assertEquals(menuItem.basePrice + 3_000L + 2_000L, item.priceSnapshot)
    }

    @Test
    fun `changing menu item basePrice after confirm does not affect saved priceSnapshot`() = runTest {
        val cart = listOf(CartItem(menuItem, 1, emptyList()))
        val billId = useCase(cart, OrderDestination.GrabAndGo).getOrThrow()
        val savedItem = orderRepository.items.values.first { it.billId == billId }
        val originalSnapshot = savedItem.priceSnapshot

        menuRepository.items[menuItem.id] = menuItem.copy(basePrice = 99_999L)

        val reread = orderRepository.items.values.first { it.billId == billId }
        assertEquals(originalSnapshot, reread.priceSnapshot)
    }

    @Test
    fun `changing option priceDelta after confirm does not affect saved selectedVariants`() = runTest {
        val variant = VariantSelection(groupId = "g1", groupName = "Size", optionId = "o1", optionName = "Large", priceDelta = 3_000L)
        val cart = listOf(CartItem(menuItem, 1, listOf(variant)))
        val billId = useCase(cart, OrderDestination.GrabAndGo).getOrThrow()

        val option = VariantOption(id = "o1", variantGroupId = "g1", name = "Large", priceDelta = 50_000L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev")
        menuRepository.variantOptions[option.id] = option

        val savedItem = orderRepository.items.values.first { it.billId == billId }
        assertEquals(3_000L, savedItem.selectedVariants.first().priceDelta)
    }

    @Test
    fun `nameSnapshot is captured at confirm time and unaffected by later rename`() = runTest {
        val cart = listOf(CartItem(menuItem, 1, emptyList()))
        val billId = useCase(cart, OrderDestination.GrabAndGo).getOrThrow()

        menuRepository.items[menuItem.id] = menuItem.copy(name = "Renamed Dish")

        val savedItem = orderRepository.items.values.first { it.billId == billId }
        assertEquals("Nasi Goreng", savedItem.nameSnapshot)
    }
}
