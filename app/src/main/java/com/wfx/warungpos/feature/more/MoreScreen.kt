package com.wfx.warungpos.feature.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.core.common.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    state: MoreUiState,
    onLock: () -> Unit,
    onNavigateToMenuManagement: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
    onNavigateToExpenseCategories: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToShiftClose: () -> Unit,
    onNavigateToShiftHistory: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToStockBatch: () -> Unit,
    onNavigateToOpname: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLockDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("More") }) },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = state.username.ifBlank { "No user" },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = when (state.userRole) {
                                    UserRole.OWNER -> "Owner"
                                    UserRole.STAFF -> "Staff"
                                    UserRole.NONE -> "Guest"
                                }
                            )
                        },
                    )
                }
                HorizontalDivider()
            }

            item {
                MoreSectionHeader("Day")
                MoreItem("Day History", Icons.Default.TableBar, onClick = onNavigateToShiftHistory)
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            }

            item {
                MoreSectionHeader("Expenses")
                MoreItem("Expense Log", Icons.Default.Payments, onClick = onNavigateToExpenses)
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            }

            item {
                MoreSectionHeader("Stock")
                MoreItem("Stock", Icons.Default.Restaurant, onClick = onNavigateToStock, badgeCount = state.lowStockCount)
                MoreItem("Stock Batches", Icons.Default.Restaurant, onClick = onNavigateToStockBatch)
                MoreItem("Stock Opname", Icons.Default.Restaurant, onClick = onNavigateToOpname)
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            }

            if (state.userRole == UserRole.OWNER) {
                item {
                    MoreSectionHeader("Settings")
                    MoreItem("Menu Management", Icons.Default.Restaurant, onClick = onNavigateToMenuManagement)
                    MoreItem("Payment Methods", Icons.Default.Payments, onClick = onNavigateToPaymentMethods)
                    MoreItem("Expense Categories", Icons.Default.Payments, onClick = onNavigateToExpenseCategories)
                    MoreItem("Language", Icons.Default.Language, onClick = onNavigateToLanguage)
                    MoreItem("Close Day", Icons.Default.TableBar, onClick = onNavigateToShiftClose)
                    MoreItem("About", Icons.Default.Info, onClick = onNavigateToAbout)
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showLockDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.padding(start = 8.dp))
                    Text("Lock App")
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = { Text("Lock App") },
            text = { Text("Lock the app and return to the PIN screen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLockDialog = false
                        onLock()
                    },
                ) { Text("Lock") }
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun MoreSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun MoreItem(title: String, icon: ImageVector, onClick: () -> Unit, badgeCount: Int = 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (badgeCount > 0) {
                Spacer(Modifier.width(8.dp))
                Badge { Text(badgeCount.toString()) }
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
