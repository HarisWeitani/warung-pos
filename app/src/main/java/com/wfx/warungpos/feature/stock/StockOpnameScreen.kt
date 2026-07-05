package com.wfx.warungpos.feature.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.wfx.warungpos.core.common.VarianceReason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOpnameScreen(
    state: StockOpnameUiState,
    onStart: () -> Unit,
    onCountedQtyChange: (stockItemId: String, value: String) -> Unit,
    onReasonChange: (stockItemId: String, reason: VarianceReason) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Stock Opname") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.inProgress == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No opname session in progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onStart, enabled = !state.isLoading) { Text("Start Opname") }
                    state.error?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            val variantCount = state.lines.count { it.variance != 0.0 }
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text(
                    text = if (variantCount == 0) "All ${state.lines.size} items match system quantity"
                    else "$variantCount of ${state.lines.size} items have a variance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.lines, key = { it.stockItemId }) { line ->
                        OpnameLineRow(
                            line = line,
                            onCountedQtyChange = { onCountedQtyChange(line.stockItemId, it) },
                            onReasonChange = { onReasonChange(line.stockItemId, it) },
                        )
                        HorizontalDivider()
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }

                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                Button(
                    onClick = onSubmit,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) { Text("Submit Opname") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpnameLineRow(
    line: OpnameLineUi,
    onCountedQtyChange: (String) -> Unit,
    onReasonChange: (VarianceReason) -> Unit,
) {
    var reasonExpanded by remember { mutableStateOf(false) }
    val hasVariance = line.variance != 0.0

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(line.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        text = "System: ${formatQty(line.systemQty)} ${line.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hasVariance) {
                    val sign = if (line.variance > 0) "+" else ""
                    Text(
                        text = "$sign${formatQty(line.variance)} ${line.unit}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = line.countedQty,
                    onValueChange = onCountedQtyChange,
                    label = { Text("Counted") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )

                if (hasVariance) {
                    Spacer(Modifier.width(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = reasonExpanded,
                        onExpandedChange = { reasonExpanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = line.reason?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Reason") },
                            isError = line.reason == null,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(expanded = reasonExpanded, onDismissRequest = { reasonExpanded = false }) {
                            VarianceReason.entries.forEach { reason ->
                                DropdownMenuItem(
                                    text = { Text(reason.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = { onReasonChange(reason); reasonExpanded = false },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
