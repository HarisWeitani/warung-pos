package com.wfx.warungpos.feature.stock

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.domain.model.StockItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    state: StockUiState,
    onAddItem: () -> Unit,
    onItemClick: (StockItem) -> Unit,
    onDismissSheet: () -> Unit,
    onNameChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onReorderPointChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Stock") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = "Add stock item")
            }
        },
    ) { padding ->
        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No stock items yet. Tap + to add one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(state.items, key = { it.id }) { item ->
                    StockItemRow(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }

    if (state.form.isOpen) {
        ModalBottomSheet(onDismissRequest = onDismissSheet, sheetState = sheetState) {
            StockItemForm(
                form = state.form,
                onNameChange = onNameChange,
                onUnitChange = onUnitChange,
                onReorderPointChange = onReorderPointChange,
                onSave = onSave,
                onDismiss = onDismissSheet,
            )
        }
    }
}

@Composable
private fun StockItemRow(item: StockItem, onClick: () -> Unit) {
    val lowStock = item.currentQty <= item.reorderPoint
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (lowStock) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Low stock",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Text(
                text = "Reorder at ${formatQty(item.reorderPoint)} ${item.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${formatQty(item.currentQty)} ${item.unit}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockItemForm(
    form: StockFormState,
    onNameChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onReorderPointChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (form.editingId == null) "Add Stock Item" else "Edit Stock Item", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = form.unit,
            onValueChange = onUnitChange,
            label = { Text("Unit (e.g. kg, pcs)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = form.reorderPoint,
            onValueChange = onReorderPointChange,
            label = { Text("Low-stock threshold") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        form.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = onSave,
                enabled = form.name.isNotBlank() && form.unit.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text("Save") }
        }
    }
}
