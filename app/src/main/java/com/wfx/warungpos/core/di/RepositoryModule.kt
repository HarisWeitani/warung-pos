package com.wfx.warungpos.core.di

import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SessionProvider
import com.wfx.warungpos.data.repository.BillRepositoryImpl
import com.wfx.warungpos.data.repository.ExpenseRepositoryImpl
import com.wfx.warungpos.data.repository.MenuRepositoryImpl
import com.wfx.warungpos.data.repository.OrderRepositoryImpl
import com.wfx.warungpos.data.repository.PaymentRepositoryImpl
import com.wfx.warungpos.data.repository.ReportRepositoryImpl
import com.wfx.warungpos.data.repository.ShiftRepositoryImpl
import com.wfx.warungpos.data.repository.StockRepositoryImpl
import com.wfx.warungpos.data.repository.TableRepositoryImpl
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.ExpenseRepository
import com.wfx.warungpos.domain.repository.MenuRepository
import com.wfx.warungpos.domain.repository.OrderRepository
import com.wfx.warungpos.domain.repository.PaymentRepository
import com.wfx.warungpos.domain.repository.ReportRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import com.wfx.warungpos.domain.repository.StockRepository
import com.wfx.warungpos.domain.repository.TableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindMenuRepository(impl: MenuRepositoryImpl): MenuRepository
    @Binds @Singleton abstract fun bindTableRepository(impl: TableRepositoryImpl): TableRepository
    @Binds @Singleton abstract fun bindBillRepository(impl: BillRepositoryImpl): BillRepository
    @Binds @Singleton abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository
    @Binds @Singleton abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository
    @Binds @Singleton abstract fun bindShiftRepository(impl: ShiftRepositoryImpl): ShiftRepository
    @Binds @Singleton abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository
    @Binds @Singleton abstract fun bindStockRepository(impl: StockRepositoryImpl): StockRepository
    @Binds @Singleton abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository
    @Binds @Singleton abstract fun bindSessionProvider(impl: SessionManager): SessionProvider
}
