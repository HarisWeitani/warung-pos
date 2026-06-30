package com.wfx.warungpos.feature.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.core.util.CurrencyFormatter
import com.wfx.warungpos.domain.model.MenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuManagementScreen(
    state: MenuManagementUiState,
    onToggleSoldOut: (MenuItem) -> Unit,
    onRequestHide: (MenuItem) -> Unit,
    onDismissHide: () -> Unit,
    onConfirmHide: () -> Unit,
    onItemClick: (MenuItem) -> Unit,
    onAddItem: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    state.itemPendingHide?.let { item ->
        AlertDialog(
            onDismissRequest = onDismissHide,
            title = { Text("Hide \"${item.name}\"?") },
            text = {
                Text(
                    if (state.itemInOpenBillWarning) {
                        "This item is in one or more open bills. Hiding it will remove it from the order screen but won't affect those bills."
                    } else {
                        "This item will no longer appear on the order screen."
                    }
                )
            },
            confirmButton = { TextButton(onClick = onConfirmHide) { Text("Hide") } },
            dismissButton = { TextButton(onClick = onDismissHide) { Text("Cancel") } },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Menu Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            state.categories.forEach { category ->
                val items = state.itemsByCategory[category.id].orEmpty()
                item {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(items, key = { it.id }) { menuItem ->
                    MenuManagementRow(
                        item = menuItem,
                        onToggleSoldOut = { onToggleSoldOut(menuItem) },
                        onHide = { onRequestHide(menuItem) },
                        onClick = { onItemClick(menuItem) },
                    )
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
            }
        }
    }
}

@Composable
private fun MenuManagementRow(
    item: MenuItem,
    onToggleSoldOut: () -> Unit,
    onHide: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                CurrencyFormatter.format(item.basePrice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.isSoldOut) {
            AssistChip(onClick = onToggleSoldOut, label = { Text("Sold Out") })
        } else {
            Switch(checked = !item.isSoldOut, onCheckedChange = { onToggleSoldOut() })
        }
        IconButton(onClick = onHide) {
            Icon(Icons.Default.VisibilityOff, contentDescription = "Hide item")
        }
    }
}
