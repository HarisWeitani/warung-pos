package com.wfx.warungpos

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.wfx.warungpos.feature.auth.PinScreen
import com.wfx.warungpos.feature.auth.PinViewModel
import com.wfx.warungpos.feature.auth.UpdateRequiredScreen
import com.wfx.warungpos.feature.sync.SyncStatusBar
import com.wfx.warungpos.ui.theme.WarungPosTheme

private val bottomNavRoutes = listOf(OrderRoute, ReportsRoute, MoreRoute)

@Composable
fun WarungPosApp(
    viewModel: AppViewModel = hiltViewModel(),
) {
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val versionGateState by viewModel.versionGateState.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    // DEFECT-015: the language preference is applied at the framework level via
    // AppCompatDelegate.setApplicationLocales() (WarungPosApplication.onCreate() for cold start,
    // LanguageSettingsViewModel.setLanguage() for an in-session change, which recreates this
    // Activity) — by the time this composes, LocalContext/LocalConfiguration already reflect it
    // natively, so stringResource() just works with no manual Context/Configuration override
    // needed here. A prior hand-rolled ContextThemeWrapper override lived here and silently
    // failed to actually change what stringResource() resolved.

    WarungPosTheme {
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
                    // pinVm is Activity-scoped (see note above) and survives across Lock App
                    // cycles within a process, so its mode must be re-derived from live
                    // registration state on every re-entry here, not just at construction —
                    // otherwise a lock immediately after first-run registration reuses the
                    // stale pre-registration REGISTER mode. See DEFECT-001.
                    LaunchedEffect(Unit) { pinVm.refreshMode() }
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

@Composable
private fun MainApp(userRole: UserRole) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    val showBottomNav = bottomNavRoutes.any { currentDestination?.hasRoute(it::class) == true }

    Scaffold(
        // Each screen under AppNavGraph owns its own Scaffold/TopAppBar and already
        // handles the status bar inset; consuming it here too would double the top gap.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
