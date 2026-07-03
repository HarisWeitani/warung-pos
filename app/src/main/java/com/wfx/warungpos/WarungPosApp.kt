package com.wfx.warungpos

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import com.wfx.warungpos.feature.auth.PinScreen
import com.wfx.warungpos.feature.auth.PinViewModel
import com.wfx.warungpos.feature.auth.UpdateRequiredScreen
import com.wfx.warungpos.feature.sync.SyncStatusBar
import com.wfx.warungpos.ui.theme.WarungPosTheme
import java.util.Locale

private val bottomNavRoutes = listOf(OrderRoute, TablesRoute, ReportsRoute, MoreRoute)

@Composable
fun WarungPosApp(
    viewModel: AppViewModel = hiltViewModel(),
) {
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val versionGateState by viewModel.versionGateState.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    // ContextThemeWrapper (not createConfigurationContext) preserves the ContextWrapper chain
    // back to the host Activity — hiltViewModel() and other Activity-context lookups (e.g. in
    // SyncStatusBar below) require that chain to resolve the Hilt component / ViewModelStoreOwner.
    val baseContext = LocalContext.current
    val localizedContext = remember(baseContext, language) {
        ContextThemeWrapper(baseContext, 0).apply {
            val config = Configuration(baseContext.resources.configuration)
            config.setLocale(Locale(language))
            applyOverrideConfiguration(config)
        }
    }

    WarungPosTheme {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedContext.resources.configuration,
        ) {
            Column {
                SyncStatusBar()
                // The version-gate RTDB check runs in the background; we don't block the UI on it.
                // The PIN screen shows immediately (VersionGateState starts Loading, which is not
                // UpdateRequired). If the check later resolves to UpdateRequired, the update screen
                // takes over.
                when {
                    versionGateState is VersionGateState.UpdateRequired -> UpdateRequiredScreen()
                    !isUnlocked -> {
                        val pinVm: PinViewModel = hiltViewModel()
                        val pinState by pinVm.uiState.collectAsStateWithLifecycle()
                        PinScreen(
                            state = pinState,
                            onUsernameChange = pinVm::onUsernameChange,
                            onPinChange = pinVm::onPinChange,
                            onConfirmPinChange = pinVm::onConfirmPinChange,
                            onSubmit = pinVm::submit,
                        )
                    }
                    else -> MainApp(userRole = userRole)
                }
            }
        }
    }
}

@Composable
private fun MainApp(userRole: UserRole) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    val showBottomNav = bottomNavRoutes.any { currentDestination?.hasRoute(it::class) == true }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                WarungBottomNav(navController = navController, userRole = userRole)
            }
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
