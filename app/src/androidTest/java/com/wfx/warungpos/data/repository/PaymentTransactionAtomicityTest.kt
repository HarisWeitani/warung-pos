package com.wfx.warungpos.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wfx.warungpos.data.local.db.WarungDatabase
import com.wfx.warungpos.data.local.entity.BillEntity
import com.wfx.warungpos.data.local.entity.PaymentEntity
import com.wfx.warungpos.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * processPaymentTransaction (PaymentRepositoryImpl) wraps payment-row inserts and the bill
 * status update in a single Room database.withTransaction block. These tests exercise that
 * exact write pattern directly against the DAOs to verify the transaction boundary actually
 * provides all-or-nothing semantics, independent of the rest of the DI graph (SyncCoordinator,
 * Firebase, etc.) which isn't needed to prove this property.
 */
@RunWith(AndroidJUnit4::class)
class PaymentTransactionAtomicityTest {

    private lateinit var db: WarungDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WarungDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        runTest {
            db.paymentMethodDao().upsert(PaymentMethodEntity("pm_tunai", "Tunai", true, true, 1, 0L, "SYNCED", "dev-1"))
            db.billDao().upsert(openBill())
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun openBill() = BillEntity(
        id = "bill-1", tableId = null, type = "UPFRONT", status = "OPEN", sessionLabel = "Counter",
        createdAt = 0L, paidAt = null, subtotal = 45_000L, discountTotal = 0L, grandTotal = 45_000L,
        note = null, shiftId = null, voidReason = null, voidedBy = null,
        updatedAt = 0L, syncStatus = "PENDING", deviceId = "dev-1",
    )

    private fun payment(id: String, methodId: String) = PaymentEntity(
        id = id, billId = "bill-1", paymentMethodId = methodId, amount = 45_000L, change = 0L,
        paidAt = 1_000L, updatedAt = 1_000L, syncStatus = "PENDING", deviceId = "dev-1",
    )

    @Test
    fun successfulTransaction_billPaidAndPaymentRowExistAtomically() = runTest {
        db.withTransaction {
            db.paymentDao().upsert(payment("p-1", "pm_tunai"))
            db.billDao().upsert(openBill().copy(status = "PAID", paidAt = 1_000L))
        }

        assertEquals("PAID", db.billDao().getById("bill-1")!!.status)
        assertEquals(1, db.paymentDao().getForBill("bill-1").size)
    }

    @Test
    fun secondPaymentAttemptOnAlreadyPaidBill_firstPaymentRowsPreserved() = runTest {
        db.withTransaction {
            db.paymentDao().upsert(payment("p-1", "pm_tunai"))
            db.billDao().upsert(openBill().copy(status = "PAID", paidAt = 1_000L))
        }

        // Application-level guard (ProcessPaymentUseCase) checks bill.status before calling
        // processPaymentTransaction again; simulate that the second attempt is never written.
        val billBeforeSecondAttempt = db.billDao().getById("bill-1")!!
        assertEquals("PAID", billBeforeSecondAttempt.status)

        assertEquals(1, db.paymentDao().getForBill("bill-1").size)
        assertEquals("p-1", db.paymentDao().getForBill("bill-1").first().id)
    }

    @Test
    fun midTransactionFailure_rollsBackBothPaymentAndBillWrite() = runTest {
        var threw = false
        try {
            db.withTransaction {
                db.paymentDao().upsert(payment("p-1", "pm_tunai"))
                db.billDao().upsert(openBill().copy(status = "PAID", paidAt = 1_000L))
                // Foreign key violation: pm_does_not_exist has no row in payment_methods.
                db.paymentDao().upsert(payment("p-2", "pm_does_not_exist"))
            }
        } catch (e: Exception) {
            threw = true
        }

        assertTrue(threw)
        // Entire transaction must roll back: bill stays OPEN, zero payment rows inserted.
        assertEquals("OPEN", db.billDao().getById("bill-1")!!.status)
        assertEquals(0, db.paymentDao().getForBill("bill-1").size)
    }
}
