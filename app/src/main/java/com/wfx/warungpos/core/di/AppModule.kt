package com.wfx.warungpos.core.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
import com.wfx.warungpos.data.local.db.MIGRATION_1_2
import com.wfx.warungpos.data.local.db.MIGRATION_2_3
import com.wfx.warungpos.data.local.db.WarungDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    @Singleton
    fun provideWarungDatabase(@ApplicationContext context: Context): WarungDatabase =
        Room.databaseBuilder(context, WarungDatabase::class.java, "warung_pos_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides fun provideMenuCategoryDao(db: WarungDatabase): MenuCategoryDao = db.menuCategoryDao()
    @Provides fun provideMenuItemDao(db: WarungDatabase): MenuItemDao = db.menuItemDao()
    @Provides fun provideVariantDao(db: WarungDatabase): VariantDao = db.variantDao()
    @Provides fun provideShiftDao(db: WarungDatabase): ShiftDao = db.shiftDao()
    @Provides fun provideBillDao(db: WarungDatabase): BillDao = db.billDao()
    @Provides fun provideOrderItemDao(db: WarungDatabase): OrderItemDao = db.orderItemDao()
    @Provides fun providePaymentMethodDao(db: WarungDatabase): PaymentMethodDao = db.paymentMethodDao()
    @Provides fun providePaymentDao(db: WarungDatabase): PaymentDao = db.paymentDao()
    @Provides fun provideZReportDao(db: WarungDatabase): ZReportDao = db.zReportDao()
    @Provides fun provideExpenseDao(db: WarungDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideStockDao(db: WarungDatabase): StockDao = db.stockDao()
    @Provides fun provideStockOpnameDao(db: WarungDatabase): StockOpnameDao = db.stockOpnameDao()
    @Provides fun provideReportQueryDao(db: WarungDatabase): ReportQueryDao = db.reportQueryDao()
}
