package com.wfx.warungpos.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.wfx.warungpos.feature.reports.DashboardScreen
import com.wfx.warungpos.feature.reports.DashboardViewModel
import com.wfx.warungpos.feature.reports.ReportsScreen
import com.wfx.warungpos.feature.reports.ReportsViewModel
import com.wfx.warungpos.feature.settings.AboutScreen
import com.wfx.warungpos.feature.settings.ExpenseCategorySettingsScreen
import com.wfx.warungpos.feature.settings.LanguageSettingsScreen
import com.wfx.warungpos.feature.settings.LanguageSettingsViewModel
import com.wfx.warungpos.feature.settings.PaymentMethodSettingsScreen
import com.wfx.warungpos.feature.settings.PaymentMethodSettingsViewModel
import com.wfx.warungpos.feature.settings.SettingsScreen
import com.wfx.warungpos.feature.settings.TableSettingsScreen
import com.wfx.warungpos.feature.settings.TableSettingsViewModel
import com.wfx.warungpos.feature.shift.ShiftCloseScreen
import com.wfx.warungpos.feature.shift.ShiftCloseViewModel
import com.wfx.warungpos.feature.shift.ShiftOpenScreen
import com.wfx.warungpos.feature.shift.ShiftOpenViewModel
import com.wfx.warungpos.feature.shift.ZReportScreen
import com.wfx.warungpos.feature.shift.ZReportViewModel
import com.wfx.warungpos.feature.tables.TablesScreen
import com.wfx.warungpos.feature.tables.TablesViewModel

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
            LaunchedEffect(vm) {
                vm.navToTables.collect {
                    navController.navigate(TablesRoute)
                }
            }
            OrderScreen(
                state = state,
                onBillClick = { navController.navigate(BillDetailRoute(it)) },
                onShowDestinationSheet = vm::showDestinationSheet,
                onDismissDestinationSheet = vm::dismissDestinationSheet,
                onGrabAndGo = vm::createUpfrontBill,
                onNewTable = vm::onNewTableSelected,
                onExistingBillSelected = vm::onExistingBillSelected,
                onOpenShift = { navController.navigate(ShiftOpenRoute) },
            )
        }

        composable<TablesRoute> {
            val vm: TablesViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(vm) {
                vm.navToBill.collect { billId ->
                    navController.navigate(BillDetailRoute(billId))
                }
            }
            TablesScreen(
                state = state,
                onTableClick = { tableId, existingBillId -> vm.onTableTapped(tableId, existingBillId) },
                onOpenShift = { navController.navigate(ShiftOpenRoute) },
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
            )
        }

        composable<ShiftOpenRoute> {
            val vm: ShiftOpenViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isSuccess) {
                if (state.isSuccess) {
                    navController.navigate(OrderRoute) {
                        popUpTo<OrderRoute> { inclusive = false }
                    }
                }
            }
            ShiftOpenScreen(
                state = state,
                onFloatChange = vm::onFloatChange,
                onOpenShift = vm::openShift,
                onBack = { navController.popBackStack() },
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
                onBack = {
                    navController.navigate(OrderRoute) {
                        popUpTo<OrderRoute> { inclusive = false }
                    }
                },
            )
        }

        composable<ReportsRoute> {
            if (userRole == UserRole.STAFF) {
                LaunchedEffect(Unit) {
                    navController.navigate(OrderRoute) {
                        popUpTo<OrderRoute> { inclusive = false }
                    }
                }
            } else {
                val vm: ReportsViewModel = hiltViewModel()
                val state by vm.uiState.collectAsStateWithLifecycle()
                ReportsScreen(
                    state = state,
                    onNavigateToDashboard = { navController.navigate(DashboardRoute) },
                )
            }
        }

        composable<DashboardRoute> {
            val vm: DashboardViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            DashboardScreen(
                state = state,
                onBack = { navController.popBackStack() },
            )
        }

        composable<MoreRoute> {
            val vm: MoreViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            MoreScreen(
                state = state,
                onSignOut = vm::signOut,
                onNavigateToMenuManagement = { navController.navigate(MenuManagementRoute) },
                onNavigateToTableSettings = { navController.navigate(TableSettingsRoute) },
                onNavigateToPaymentMethods = { navController.navigate(PaymentMethodSettingsRoute) },
                onNavigateToExpenseCategories = { navController.navigate(ExpenseCategorySettingsRoute) },
                onNavigateToLanguage = { navController.navigate(LanguageSettingsRoute) },
                onNavigateToAbout = { navController.navigate(AboutRoute) },
                onNavigateToShiftOpen = { navController.navigate(ShiftOpenRoute) },
                onNavigateToShiftClose = { navController.navigate(ShiftCloseRoute) },
                onNavigateToExpenses = { navController.navigate(ExpenseLogRoute) },
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
                onBack = { navController.popBackStack() },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToTables = { navController.navigate(TableSettingsRoute) },
                onNavigateToPaymentMethods = { navController.navigate(PaymentMethodSettingsRoute) },
                onNavigateToExpenseCategories = { navController.navigate(ExpenseCategorySettingsRoute) },
                onNavigateToLanguage = { navController.navigate(LanguageSettingsRoute) },
                onNavigateToAbout = { navController.navigate(AboutRoute) },
            )
        }

        composable<TableSettingsRoute> {
            val vm: TableSettingsViewModel = hiltViewModel()
            val tables by vm.tables.collectAsStateWithLifecycle()
            TableSettingsScreen(
                tables = tables,
                onAddTable = vm::addTable,
                onToggleActive = vm::toggleActive,
                onBack = { navController.popBackStack() },
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
    }
}
