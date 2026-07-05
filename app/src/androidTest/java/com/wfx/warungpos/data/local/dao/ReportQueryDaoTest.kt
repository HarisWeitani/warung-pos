package com.wfx.warungpos.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wfx.warungpos.data.local.db.WarungDatabase
import com.wfx.warungpos.data.local.entity.BillEntity
import com.wfx.warungpos.data.local.entity.MenuItemEntity
import com.wfx.warungpos.data.local.entity.OrderItemEntity
import com.wfx.warungpos.data.local.entity.PaymentEntity
import com.wfx.warungpos.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportQueryDaoTest {

    private lateinit var db: WarungDatabase
    private lateinit var reportQueryDao: ReportQueryDao
    private lateinit var billDao: BillDao
    private lateinit var orderItemDao: OrderItemDao
    private lateinit var menuItemDao: MenuItemDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var paymentMethodDao: PaymentMethodDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WarungDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reportQueryDao = db.reportQueryDao()
        billDao = db.billDao()
        orderItemDao = db.orderItemDao()
        menuItemDao = db.menuItemDao()
        paymentDao = db.paymentDao()
        paymentMethodDao = db.paymentMethodDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun bill(id: String, status: String, paidAt: Long?, grandTotal: Long) = BillEntity(
        id = id, status = status, sessionLabel = "Counter",
        createdAt = 0L, paidAt = paidAt, subtotal = grandTotal, discountTotal = 0L,
        grandTotal = grandTotal, note = null, shiftId = null, voidReason = null, voidedBy = null,
        updatedAt = 0L, syncStatus = "PENDING", deviceId = "dev-1",
    )

    @Test
    fun totalRevenueInRange_sumsThreePaidBillsInRange() = runTest {
        billDao.upsert(bill("bill-1", "PAID", paidAt = 1_000L, grandTotal = 10_000L))
        billDao.upsert(bill("bill-2", "PAID", paidAt = 2_000L, grandTotal = 20_000L))
        billDao.upsert(bill("bill-3", "PAID", paidAt = 3_000L, grandTotal = 30_000L))

        val total = reportQueryDao.totalRevenueInRange(0L, 5_000L)
        assertEquals(60_000L, total)
    }

    @Test
    fun totalRevenueInRange_excludesBillOutsideRange() = runTest {
        billDao.upsert(bill("bill-1", "PAID", paidAt = 1_000L, grandTotal = 10_000L))
        billDao.upsert(bill("bill-2", "PAID", paidAt = 2_000L, grandTotal = 20_000L))
        billDao.upsert(bill("bill-3", "PAID", paidAt = 3_000L, grandTotal = 30_000L))
        billDao.upsert(bill("bill-4", "PAID", paidAt = 99_000L, grandTotal = 99_999L))

        val total = reportQueryDao.totalRevenueInRange(0L, 5_000L)
        assertEquals(60_000L, total)
    }

    private fun menuItem(id: String, name: String) = MenuItemEntity(
        id = id, categoryId = null, name = name, basePrice = 10_000L,
        isAvailable = true, isSoldOut = false, updatedAt = 0L, syncStatus = "SYNCED", deviceId = "dev-1",
    )

    private fun orderItem(
        id: String, billId: String, menuItemId: String, qty: Int, status: String = "ORDERED",
    ) = OrderItemEntity(
        id = id, billId = billId, menuItemId = menuItemId, nameSnapshot = "Item",
        priceSnapshot = 10_000L, quantity = qty, selectedVariantsJson = "[]",
        lineTotal = 10_000L * qty, status = status, voidReason = null, voidedBy = null,
        createdAt = 0L, updatedAt = 0L, syncStatus = "PENDING", deviceId = "dev-1",
    )

    @Test
    fun getBestSellers_ranksByQuantityDescending() = runTest {
        menuItemDao.upsert(menuItem("item-a", "Item A"))
        menuItemDao.upsert(menuItem("item-b", "Item B"))
        billDao.upsert(bill("bill-1", "PAID", paidAt = 1_000L, grandTotal = 60_000L))

        // item-a: two order rows of qty 3 each = 6 total; item-b: one row of qty 5
        orderItemDao.upsert(orderItem("oi-1", "bill-1", "item-a", qty = 3))
        orderItemDao.upsert(orderItem("oi-2", "bill-1", "item-a", qty = 3))
        orderItemDao.upsert(orderItem("oi-3", "bill-1", "item-b", qty = 5))

        val sellers = reportQueryDao.getBestSellers(0L, 5_000L, limit = 10)
        assertEquals("item-a", sellers.first().menuItemId)
        assertEquals(6, sellers.first().totalQty)
        assertEquals(5, sellers.first { it.menuItemId == "item-b" }.totalQty)
    }

    @Test
    fun getBestSellers_excludesVoidedOrderItems() = runTest {
        menuItemDao.upsert(menuItem("item-a", "Item A"))
        billDao.upsert(bill("bill-1", "PAID", paidAt = 1_000L, grandTotal = 60_000L))

        orderItemDao.upsert(orderItem("oi-1", "bill-1", "item-a", qty = 3, status = "ORDERED"))
        orderItemDao.upsert(orderItem("oi-2", "bill-1", "item-a", qty = 10, status = "VOID"))

        val sellers = reportQueryDao.getBestSellers(0L, 5_000L, limit = 10)
        assertEquals(1, sellers.size)
        assertEquals(3, sellers.first().totalQty)
    }

    @Test
    fun salesByPaymentMethod_groupsByMethodWithCorrectTotals() = runTest {
        paymentMethodDao.upsert(PaymentMethodEntity("pm_tunai", "Tunai", true, true, 1, 0L, "SYNCED", "dev-1"))
        paymentMethodDao.upsert(PaymentMethodEntity("pm_qris", "QRIS", true, false, 2, 0L, "SYNCED", "dev-1"))
        billDao.upsert(bill("bill-1", "PAID", paidAt = 1_000L, grandTotal = 65_000L))

        paymentDao.upsert(PaymentEntity("p-1", "bill-1", "pm_tunai", 15_000L, 0L, 1_000L, 1_000L, "PENDING", "dev-1"))
        paymentDao.upsert(PaymentEntity("p-2", "bill-1", "pm_tunai", 20_000L, 0L, 1_000L, 1_000L, "PENDING", "dev-1"))
        paymentDao.upsert(PaymentEntity("p-3", "bill-1", "pm_qris", 30_000L, 0L, 1_000L, 1_000L, "PENDING", "dev-1"))

        val breakdown = paymentDao.sumByMethodInRange(0L, 5_000L)
        assertEquals(2, breakdown.size)
        assertEquals(35_000L, breakdown.first { it.paymentMethodId == "pm_tunai" }.total)
        assertEquals(30_000L, breakdown.first { it.paymentMethodId == "pm_qris" }.total)
    }
}
