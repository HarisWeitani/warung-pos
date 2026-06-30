package com.wfx.warungpos.feature.order

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.core.util.CurrencyFormatter
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.domain.model.VariantSelection
import com.wfx.warungpos.feature.order.component.VariantSelectionSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    state: BillDetailUiState,
    variantSheetState: VariantSheetState?,
    onBack: () -> Unit,
    onPay: () -> Unit,
    onMenuItemTapped: (MenuItem) -> Unit,
    onConfirmVariantSelection: (List<VariantSelection>) -> Unit,
    onDismissVariantSheet: () -> Unit,
    onVoidItem: (itemId: String, reason: VoidReason, note: String?) -> Unit,
    onVoidBill: () -> Unit,
    onDismissVoidError: () -> Unit,
    onCategorySelect: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bill = state.bill
    val activeItems = state.orderItems.filter { it.status != OrderItemStatus.VOID }
    var itemPendingVoid by remember { mutableStateOf<OrderItem?>(null) }
    var showVoidBillDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    variantSheetState?.let { sheetState ->
        VariantSelectionSheet(
            menuItem = sheetState.menuItem,
            groups = sheetState.groups,
            optionsByGroup = sheetState.optionsByGroup,
            onConfirm = onConfirmVariantSelection,
            onDismiss = onDismissVariantSheet,
        )
    }

    itemPendingVoid?.let { item ->
        VoidItemDialog(
            itemName = item.nameSnapshot,
            onConfirm = { reason, note ->
                onVoidItem(item.id, reason, note)
                itemPendingVoid = null
            },
            onDismiss = { itemPendingVoid = null },
        )
    }

    if (showVoidBillDialog) {
        AlertDialog(
            onDismissRequest = { showVoidBillDialog = false },
            title = { Text("Void Bill") },
            text = { Text("This will void the entire bill and all its items. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showVoidBillDialog = false; onVoidBill() }) { Text("Void Bill") }
            },
            dismissButton = {
                TextButton(onClick = { showVoidBillDialog = false }) { Text("Cancel") }
            },
        )
    }

    state.voidError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissVoidError,
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = onDismissVoidError) { Text("OK") } },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(bill?.sessionLabel ?: "Order Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isOwner && bill?.status?.name == "OPEN") {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Void Bill") },
                                onClick = { showMenu = false; showVoidBillDialog = true },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (bill != null && bill.status.name == "OPEN") {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = CurrencyFormatter.format(bill.grandTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Button(
                            onClick = onPay,
                            enabled = activeItems.isNotEmpty(),
                        ) {
                            Text("Pay")
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            // Order items header
            item {
                Text(
                    text = "Order Items",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (activeItems.isEmpty()) {
                item {
                    Text(
                        text = "No items yet. Add from the menu below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(activeItems, key = { it.id }) { item ->
                    OrderItemRow(
                        item = item,
                        onVoid = { itemPendingVoid = item },
                        canVoid = bill?.status?.name == "OPEN",
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Menu picker section
            item {
                Text(
                    text = "Add Items",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.categories.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.categories.forEach { cat ->
                            FilterChip(
                                selected = state.selectedCategoryId == cat.id,
                                onClick = { onCategorySelect(cat.id) },
                                label = { Text(cat.name) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (state.menuItems.isEmpty()) {
                item {
                    Text(
                        text = "No items available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(state.menuItems, key = { it.id }) { menuItem ->
                    MenuItemRow(
                        item = menuItem,
                        onAdd = { onMenuItemTapped(menuItem) },
                        enabled = bill?.status?.name == "OPEN",
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItem, onVoid: () -> Unit, canVoid: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nameSnapshot,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${item.quantity} × ${CurrencyFormatter.format(item.priceSnapshot)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = CurrencyFormatter.format(item.lineTotal),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (canVoid) {
            IconButton(onClick = onVoid) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Void item",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MenuItemRow(item: MenuItem, onAdd: () -> Unit, enabled: Boolean) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = { if (enabled) onAdd() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = CurrencyFormatter.format(item.basePrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private val VOID_REASONS = listOf(
    VoidReason.CUSTOMER_CHANGE,
    VoidReason.KITCHEN_ERROR,
    VoidReason.ITEM_UNAVAILABLE,
    VoidReason.TEST,
    VoidReason.OTHER,
)

private fun VoidReason.label(): String = when (this) {
    VoidReason.CUSTOMER_CHANGE -> "Customer Changed Mind"
    VoidReason.KITCHEN_ERROR -> "Kitchen Error"
    VoidReason.ITEM_UNAVAILABLE -> "Item Unavailable"
    VoidReason.TEST -> "Test Order"
    VoidReason.OTHER -> "Other"
}

@Composable
private fun VoidItemDialog(
    itemName: String,
    onConfirm: (VoidReason, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedReason by remember { mutableStateOf(VoidReason.CUSTOMER_CHANGE) }
    var note by remember { mutableStateOf("") }
    val noteRequired = selectedReason == VoidReason.OTHER
    val noteValid = !noteRequired || note.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Void \"$itemName\"") },
        text = {
            Column {
                VOID_REASONS.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason })
                        Spacer(Modifier.width(8.dp))
                        Text(reason.label())
                    }
                }
                if (noteRequired) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (required)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedReason, note.ifBlank { null }) },
                enabled = noteValid,
            ) { Text("Void Item") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
