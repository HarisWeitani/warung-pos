package com.wfx.warungpos.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.feature.expense.ExpenseLogScreen
import com.wfx.warungpos.feature.expense.ExpenseLogViewModel
import com.wfx.warungpos.feature.kitchen.KitchenQueueScreen
import com.wfx.warungpos.feature.kitchen.KitchenQueueViewModel
import com.wfx.warungpos.feature.menu.MenuItemEditScreen
import com.wfx.warungpos.feature.menu.MenuItemEditViewModel
import com.wfx.warungpos.feature.menu.MenuManagementScreen
import com.wfx.warungpos.feature.menu.MenuManagementViewModel
import com.wfx.warungpos.feature.more.MoreScreen
import com.wfx.warungpos.feature.more.MoreViewModel
import com.wfx.warungpos.feature.order.BillDetailScreen
import com.wfx.warungpos.feature.order.BillDetailViewModel
import com.wfx.warungpos.feature.order.OrderScreen
import com.wfx.warungpos.feature.order.OrderViewModel
import com.wfx.warungpos.feature.payment.PaymentScreen
import com.wfx.warungpos.feature.payment.PaymentViewModel
import com.wfx.warungpos.feature.reports.BestSellerScreen
import com.wfx.warungpos.feature.reports.DashboardScreen
import com.wfx.warungpos.feature.reports.DashboardViewModel
import com.wfx.warungpos.feature.reports.ReportScreen
import com.wfx.warungpos.feature.reports.ReportViewModel
import com.wfx.warungpos.feature.reports.ReportsScreen
import com.wfx.warungpos.feature.reports.ReportsViewModel
import com.wfx.warungpos.feature.settings.AboutScreen
import com.wfx.warungpos.feature.settings.ExpenseCategorySettingsScreen
import com.wfx.warungpos.feature.settings.LanguageSettingsScreen
import com.wfx.warungpos.feature.settings.LanguageSettingsViewModel
import com.wfx.warungpos.feature.settings.PaymentMethodSettingsScreen
import com.wfx.warungpos.feature.settings.PaymentMethodSettingsViewModel
import com.wfx.warungpos.feature.settings.SettingsScreen
import com.wfx.warungpos.feature.shift.ShiftCloseScreen
import com.wfx.warungpos.feature.shift.ShiftCloseViewModel
import com.wfx.warungpos.feature.shift.ShiftHistoryScreen
import com.wfx.warungpos.feature.shift.ShiftHistoryViewModel
import com.wfx.warungpos.feature.shift.ZReportScreen
import com.wfx.warungpos.feature.shift.ZReportViewModel
import com.wfx.warungpos.feature.stock.StockBatchScreen
import com.wfx.warungpos.feature.stock.StockBatchViewModel
import com.wfx.warungpos.feature.stock.StockOpnameScreen
import com.wfx.warungpos.feature.stock.StockOpnameViewModel
import com.wfx.warungpos.feature.stock.StockScreen
import com.wfx.warungpos.feature.stock.StockViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    userRole: UserRole,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = OrderRoute,
        modifier = modifier,
    ) {
        composable<OrderRoute> {
            val vm: OrderViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(vm) {
                vm.navToBill.collect { billId ->
                    navController.navigate(BillDetailRoute(billId))
                }
            }
            OrderScreen(
                state = state,
                onBillClick = { navController.navigate(BillDetailRoute(it)) },
                onNewBill = vm::createBill,
            )
        }

        composable<BillDetailRoute> {
            val vm: BillDetailViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isBillPaid, state.billVoided) {
                if (state.isBillPaid || state.billVoided) {
                    navController.popBackStack()
                }
            }
            val variantSheetState by vm.variantSheetState.collectAsStateWithLifecycle()
            BillDetailScreen(
                state = state,
                variantSheetState = variantSheetState,
                onBack = { navController.popBackStack() },
                onPay = {
                    state.bill?.id?.let { billId ->
                        navController.navigate(PaymentRoute(billId))
                    }
                },
                onMenuItemTapped = vm::onMenuItemTapped,
                onConfirmVariantSelection = vm::confirmVariantSelection,
                onDismissVariantSheet = vm::dismissVariantSheet,
                onVoidItem = vm::voidItem,
                onVoidBill = vm::voidBill,
                onDismissVoidError = vm::dismissVoidError,
                onCategorySelect = vm::selectCategory,
            )
        }

        composable<PaymentRoute> {
            val vm: PaymentViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isSuccess) {
                if (state.isSuccess) {
                    navController.navigate(OrderRoute) {
                        popUpTo<OrderRoute> { inclusive = false }
                    }
                }
            }
            PaymentScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onSelectMethod = vm::selectMethod,
                onTenderChange = vm::onTenderChange,
                onConfirm = vm::confirmPayment,
                onDismissError = vm::dismissError,
            )
        }

        composable<ShiftCloseRoute> {
            val vm: ShiftCloseViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.closedShiftId) {
                state.closedShiftId?.let { shiftId ->
                    navController.navigate(ZReportRoute(shiftId)) {
                        popUpTo<ShiftCloseRoute> { inclusive = true }
                    }
                }
            }
            ShiftCloseScreen(
                state = state,
                onFloatChange = vm::onFloatChange,
                onCloseShift = vm::closeShift,
                onBack = { navController.popBackStack() },
            )
        }

        composable<ZReportRoute> {
            val vm: ZReportViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            ZReportScreen(
                state = state,
                onBack = { navController.popBackStack() },
            )
        }

        composable<ShiftHistoryRoute> {
            val vm: ShiftHistoryViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            ShiftHistoryScreen(
                state = state,
                onShiftClick = { shiftId -> navController.navigate(ZReportRoute(shiftId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<ReportsRoute> {
            val vm: ReportsViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            ReportsScreen(
                state = state,
                onNavigateToDashboard = { navController.navigate(DashboardRoute) },
            )
        }

        composable<DashboardRoute> {
            val vm: DashboardViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            DashboardScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onNavigateToFullReport = { navController.navigate(FullReportRoute) },
            )
        }

        composable<FullReportRoute> {
            val vm: ReportViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            ReportScreen(
                state = state,
                shareEvent = vm.shareEvent,
                onSelectMode = vm::selectMode,
                onSelectCustomRange = vm::selectCustomRange,
                onShare = vm::share,
                onNavigateToBestSellers = { navController.navigate(BestSellerRoute) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<BestSellerRoute> { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(FullReportRoute) }
            val vm: ReportViewModel = hiltViewModel(parentEntry)
            val state by vm.uiState.collectAsStateWithLifecycle()
            BestSellerScreen(
                bestSellers = state.reportData?.bestSellers ?: emptyList(),
                onBack = { navController.popBackStack() },
            )
        }

        composable<MoreRoute> {
            val vm: MoreViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            MoreScreen(
                state = state,
                onLock = vm::lock,
                onNavigateToMenuManagement = { navController.navigate(MenuManagementRoute) },
                onNavigateToPaymentMethods = { navController.navigate(PaymentMethodSettingsRoute) },
                onNavigateToExpenseCategories = { navController.navigate(ExpenseCategorySettingsRoute) },
                onNavigateToLanguage = { navController.navigate(LanguageSettingsRoute) },
                onNavigateToAbout = { navController.navigate(AboutRoute) },
                onNavigateToShiftClose = { navController.navigate(ShiftCloseRoute) },
                onNavigateToShiftHistory = { navController.navigate(ShiftHistoryRoute) },
                onNavigateToExpenses = { navController.navigate(ExpenseLogRoute) },
                onNavigateToStock = { navController.navigate(StockRoute) },
                onNavigateToStockBatch = { navController.navigate(StockBatchRoute) },
                onNavigateToOpname = { navController.navigate(OpnameRoute) },
                onNavigateToKitchenQueue = { navController.navigate(KitchenQueueRoute) },
            )
        }

        composable<KitchenQueueRoute> {
            val vm: KitchenQueueViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            KitchenQueueScreen(
                state = state,
                onMarkDone = vm::markDone,
            )
        }

        composable<ExpenseLogRoute> {
            val vm: ExpenseLogViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            val note by vm.note.collectAsStateWithLifecycle()
            ExpenseLogScreen(
                state = state,
                note = note,
                onNoteChange = vm::onNoteChange,
                onShowAdd = vm::showAddSheet,
                onDismissSheet = vm::dismissSheet,
                onCategoryChange = vm::onCategoryChange,
                onAmountChange = vm::onAmountChange,
                onSave = vm::saveExpense,
            )
        }

        composable<LoginRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Login — handled at app level")
            }
        }

        composable<MenuManagementRoute> {
            val vm: MenuManagementViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            MenuManagementScreen(
                state = state,
                onToggleSoldOut = vm::toggleSoldOut,
                onRequestHide = vm::requestHide,
                onDismissHide = vm::dismissHideRequest,
                onConfirmHide = vm::confirmHide,
                onItemClick = { item -> navController.navigate(MenuItemEditRoute(item.id)) },
                onAddItem = { navController.navigate(MenuItemEditRoute()) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<MenuItemEditRoute> {
            val vm: MenuItemEditViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            MenuItemEditScreen(
                state = state,
                onNameChange = vm::onNameChange,
                onCategoryChange = vm::onCategoryChange,
                onPriceChange = vm::onPriceChange,
                onSave = vm::save,
                onAddVariantGroup = vm::addVariantGroup,
                onUpdateGroup = vm::updateGroup,
                onDeleteGroup = vm::deleteGroup,
                onAddOption = vm::addOption,
                onUpdateOption = vm::updateOption,
                onDeleteOption = vm::deleteOption,
                onAddIngredient = vm::addIngredient,
                onUpdateIngredient = vm::updateIngredient,
                onDeleteIngredient = vm::deleteIngredient,
                onBack = { navController.popBackStack() },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPaymentMethods = { navController.navigate(PaymentMethodSettingsRoute) },
                onNavigateToExpenseCategories = { navController.navigate(ExpenseCategorySettingsRoute) },
                onNavigateToLanguage = { navController.navigate(LanguageSettingsRoute) },
                onNavigateToAbout = { navController.navigate(AboutRoute) },
            )
        }

        composable<PaymentMethodSettingsRoute> {
            val vm: PaymentMethodSettingsViewModel = hiltViewModel()
            val methods by vm.methods.collectAsStateWithLifecycle()
            PaymentMethodSettingsScreen(
                methods = methods,
                onToggleActive = vm::toggleActive,
                onBack = { navController.popBackStack() },
            )
        }

        composable<ExpenseCategorySettingsRoute> {
            ExpenseCategorySettingsScreen(onBack = { navController.popBackStack() })
        }

        composable<LanguageSettingsRoute> {
            val vm: LanguageSettingsViewModel = hiltViewModel()
            val language by vm.language.collectAsStateWithLifecycle()
            LanguageSettingsScreen(
                currentLanguage = language,
                onSelectLanguage = vm::setLanguage,
                onBack = { navController.popBackStack() },
            )
        }

        composable<AboutRoute> {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable<StockRoute> {
            val vm: StockViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            StockScreen(
                state = state,
                onAddItem = vm::showAddSheet,
                onItemClick = vm::showEditSheet,
                onDismissSheet = vm::dismissSheet,
                onNameChange = vm::onNameChange,
                onUnitChange = vm::onUnitChange,
                onReorderPointChange = vm::onReorderPointChange,
                onSave = vm::save,
                onBack = { navController.popBackStack() },
            )
        }

        composable<StockBatchRoute> {
            val vm: StockBatchViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            StockBatchScreen(
                state = state,
                onAddBatch = vm::showAddSheet,
                onDismissSheet = vm::dismissSheet,
                onStockItemChange = vm::onStockItemChange,
                onQtyChange = vm::onQtyChange,
                onCostChange = vm::onCostChange,
                onSave = vm::save,
                onBack = { navController.popBackStack() },
            )
        }

        composable<OpnameRoute> {
            val vm: StockOpnameViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            StockOpnameScreen(
                state = state,
                onStart = vm::startOpname,
                onCountedQtyChange = vm::onCountedQtyChange,
                onReasonChange = vm::onReasonChange,
                onSubmit = vm::submit,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
