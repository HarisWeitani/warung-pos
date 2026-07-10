package com.wfx.warungpos.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wfx.warungpos.data.local.dao.BillDao
import com.wfx.warungpos.data.local.dao.ExpenseDao
import com.wfx.warungpos.data.local.dao.MenuCategoryDao
import com.wfx.warungpos.data.local.dao.MenuItemDao
import com.wfx.warungpos.data.local.dao.OrderItemDao
import com.wfx.warungpos.data.local.dao.PaymentDao
import com.wfx.warungpos.data.local.dao.PaymentMethodDao
import com.wfx.warungpos.data.local.dao.ReportQueryDao
import com.wfx.warungpos.data.local.dao.ShiftDao
import com.wfx.warungpos.data.local.dao.StockDao
import com.wfx.warungpos.data.local.dao.StockOpnameDao
import com.wfx.warungpos.data.local.dao.VariantDao
import com.wfx.warungpos.data.local.dao.ZReportDao
import com.wfx.warungpos.data.local.entity.BillEntity
import com.wfx.warungpos.data.local.entity.ExpenseEntity
import com.wfx.warungpos.data.local.entity.MenuCategoryEntity
import com.wfx.warungpos.data.local.entity.MenuItemEntity
import com.wfx.warungpos.data.local.entity.MenuItemIngredientEntity
import com.wfx.warungpos.data.local.entity.OrderItemEntity
import com.wfx.warungpos.data.local.entity.PaymentEntity
import com.wfx.warungpos.data.local.entity.PaymentMethodEntity
import com.wfx.warungpos.data.local.entity.PendingStockDeductionEntity
import com.wfx.warungpos.data.local.entity.ShiftEntity
import com.wfx.warungpos.data.local.entity.StockBatchEntity
import com.wfx.warungpos.data.local.entity.StockItemEntity
import com.wfx.warungpos.data.local.entity.StockOpnameEntity
import com.wfx.warungpos.data.local.entity.StockOpnameLineEntity
import com.wfx.warungpos.data.local.entity.VariantGroupEntity
import com.wfx.warungpos.data.local.entity.VariantOptionEntity
import com.wfx.warungpos.data.local.entity.ZReportEntity

@Database(
    entities = [
        MenuCategoryEntity::class,
        MenuItemEntity::class,
        VariantGroupEntity::class,
        VariantOptionEntity::class,
        ShiftEntity::class,
        BillEntity::class,
        OrderItemEntity::class,
        PaymentMethodEntity::class,
        PaymentEntity::class,
        ZReportEntity::class,
        ExpenseEntity::class,
        StockItemEntity::class,
        StockBatchEntity::class,
        MenuItemIngredientEntity::class,
        StockOpnameEntity::class,
        StockOpnameLineEntity::class,
        PendingStockDeductionEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class WarungDatabase : RoomDatabase() {
    abstract fun menuCategoryDao(): MenuCategoryDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun variantDao(): VariantDao
    abstract fun shiftDao(): ShiftDao
    abstract fun billDao(): BillDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun paymentDao(): PaymentDao
    abstract fun zReportDao(): ZReportDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun stockDao(): StockDao
    abstract fun stockOpnameDao(): StockOpnameDao
    abstract fun reportQueryDao(): ReportQueryDao
}
