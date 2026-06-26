package com.wfx.warungpos

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.core.navigation.AppNavGraph
import com.wfx.warungpos.core.navigation.MoreRoute
import com.wfx.warungpos.core.navigation.OrderRoute
import com.wfx.warungpos.core.navigation.ReportsRoute
import com.wfx.warungpos.core.navigation.TablesRoute
import com.wfx.warungpos.feature.auth.UpdateRequiredScreen
import com.wfx.warungpos.ui.theme.WarungPosTheme

@Composable
fun WarungPosApp(
    viewModel: AppViewModel = hiltViewModel(),
) {
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val versionGateState by viewModel.versionGateState.collectAsStateWithLifecycle()

    WarungPosTheme {
        when (versionGateState) {
            VersionGateState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            VersionGateState.UpdateRequired -> UpdateRequiredScreen()
            VersionGateState.Allowed -> MainApp(userRole = userRole)
        }
    }
}

@Composable
private fun MainApp(userRole: UserRole) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            WarungBottomNav(navController = navController, userRole = userRole)
        },
    ) { padding ->
        AppNavGraph(
            navController = navController,
            userRole = userRole,
            modifier = Modifier.padding(padding),
        )
    }
}

private data class BottomNavItem(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val route: Any,
)

private val alwaysVisibleItems = listOf(
    BottomNavItem(R.string.nav_order, Icons.Default.ShoppingCart, OrderRoute),
    BottomNavItem(R.string.nav_tables, Icons.Default.List, TablesRoute),
)
private val ownerOnlyItem =
    BottomNavItem(R.string.nav_reports, Icons.Default.BarChart, ReportsRoute)
private val moreItem =
    BottomNavItem(R.string.nav_more, Icons.Default.MoreVert, MoreRoute)

@Composable
private fun WarungBottomNav(
    navController: NavHostController,
    userRole: UserRole,
) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    val items = buildList {
        addAll(alwaysVisibleItems)
        if (userRole == UserRole.OWNER) add(ownerOnlyItem)
        add(moreItem)
    }

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hasRoute(item.route::class) == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo<OrderRoute> { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.labelRes),
                    )
                },
                label = { Text(stringResource(item.labelRes)) },
            )
        }
    }
}
