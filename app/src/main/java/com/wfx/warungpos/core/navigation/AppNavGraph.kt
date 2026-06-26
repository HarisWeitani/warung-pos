package com.wfx.warungpos.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wfx.warungpos.core.common.UserRole

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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Order — Coming Soon")
            }
        }
        composable<TablesRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tables — Coming Soon")
            }
        }
        composable<ReportsRoute> {
            // Role guard: STAFF may not access owner-only Reports route
            if (userRole == UserRole.STAFF) {
                LaunchedEffect(Unit) {
                    navController.navigate(OrderRoute) {
                        popUpTo<OrderRoute> { inclusive = false }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Reports — Coming Soon")
                }
            }
        }
        composable<MoreRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("More — Coming Soon")
            }
        }
        composable<LoginRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Login — Coming Soon")
            }
        }
        composable<ShiftOpenRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Open Shift — Coming Soon")
            }
        }
        composable<ShiftCloseRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Close Shift — Coming Soon")
            }
        }
        composable<ZReportRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Z-Report — Coming Soon")
            }
        }
        composable<BillDetailRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Bill Detail — Coming Soon")
            }
        }
        composable<PaymentRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Payment — Coming Soon")
            }
        }
        composable<ExpenseLogRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Expense Log — Coming Soon")
            }
        }
        composable<MenuManagementRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Menu Management — Coming Soon")
            }
        }
        composable<MenuItemEditRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Menu Item Edit — Coming Soon")
            }
        }
        composable<SettingsRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Settings — Coming Soon")
            }
        }
        composable<TableSettingsRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Table Settings — Coming Soon")
            }
        }
        composable<PaymentMethodSettingsRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Payment Methods — Coming Soon")
            }
        }
        composable<ExpenseCategorySettingsRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Expense Categories — Coming Soon")
            }
        }
        composable<LanguageSettingsRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Language — Coming Soon")
            }
        }
        composable<AboutRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("About — Coming Soon")
            }
        }
    }
}
