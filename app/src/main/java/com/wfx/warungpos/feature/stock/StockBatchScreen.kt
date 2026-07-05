package com.wfx.warungpos.feature.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.core.util.CurrencyFormatter
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.domain.model.StockBatch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockBatchScreen(
    state: StockBatchUiState,
    onAddBatch: () -> Unit,
    onDismissSheet: () -> Unit,
    onStockItemChange: (String) -> Unit,
    onQtyChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Stock Batches") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.items.isNotEmpty()) {
                FloatingActionButton(onClick = onAddBatch) {
                    Icon(Icons.Default.Add, contentDescription = "Receive stock")
                }
            }
        },
    ) { padding ->
        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Add a stock item first before receiving batches.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (state.batches.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No batches received yet. Tap + to receive stock.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(state.batches, key = { it.id }) { batch ->
                    BatchRow(batch = batch, itemName = state.itemNames[batch.stockItemId] ?: "Unknown item")
                    HorizontalDivider()
                }
            }
        }
    }

    if (state.form.isOpen) {
        ModalBottomSheet(onDismissRequest = onDismissSheet, sheetState = sheetState) {
            ReceiveBatchForm(
                state = state,
                onStockItemChange = onStockItemChange,
                onQtyChange = onQtyChange,
                onCostChange = onCostChange,
                onSave = onSave,
                onDismiss = onDismissSheet,
            )
        }
    }
}

@Composable
private fun BatchRow(batch: StockBatch, itemName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(itemName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = DateUtil.toDisplayString(batch.receivedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("+${formatQty(batch.qty)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = "@ ${CurrencyFormatter.format(batch.costPerUnit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiveBatchForm(
    state: StockBatchUiState,
    onStockItemChange: (String) -> Unit,
    onQtyChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val form = state.form
    val selectedItem = state.items.firstOrNull { it.id == form.stockItemId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Receive Stock", style = MaterialTheme.typography.titleLarge)

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedItem?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Stock Item") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.items.forEach { stockItem ->
                    DropdownMenuItem(
                        text = { Text("${stockItem.name} (${formatQty(stockItem.currentQty)} ${stockItem.unit})") },
                        onClick = { onStockItemChange(stockItem.id); expanded = false },
                    )
                }
            }
        }

        OutlinedTextField(
            value = form.qty,
            onValueChange = onQtyChange,
            label = { Text("Quantity received${selectedItem?.let { " (${it.unit})" } ?: ""}") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = form.costPerUnit,
            onValueChange = onCostChange,
            label = { Text("Cost per unit (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            prefix = { Text("Rp ") },
            modifier = Modifier.fillMaxWidth(),
        )

        form.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = onSave,
                enabled = form.stockItemId != null && (form.qty.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier.weight(1f),
            ) { Text("Save") }
        }
    }
}
