package com.wfx.warungpos.feature.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.core.util.CurrencyFormatter
import com.wfx.warungpos.core.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftCloseScreen(
    state: ShiftCloseViewModel.UiState,
    onFloatChange: (String) -> Unit,
    onCloseShift: () -> Unit,
    onBack: () -> Unit,
    onOtherShiftFloatChange: (shiftId: String, value: String) -> Unit = { _, _ -> },
    onCloseOtherShift: (shiftId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Close Day") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            state.shift?.let { shift ->
                item {
                    Text(
                        text = "Opened: ${DateUtil.toDisplayString(shift.openedAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Blocking open bills
            if (state.openBills.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Cannot close: ${state.openBills.size} open bill(s)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            state.openBills.forEach { bill ->
                                Text(
                                    text = "• ${bill.sessionLabel} — ${CurrencyFormatter.format(bill.grandTotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Day Summary", style = MaterialTheme.typography.titleMedium)
                        SummaryRow(label = "Total Revenue", value = CurrencyFormatter.format(state.totalRevenue))
                        SummaryRow(label = "Total Expenses", value = CurrencyFormatter.format(state.totalExpenses))
                        HorizontalDivider()
                        SummaryRow(
                            label = "Net",
                            value = CurrencyFormatter.format(state.totalRevenue - state.totalExpenses),
                            bold = true,
                        )
                        SummaryRow(label = "Transactions", value = state.transactionCount.toString())
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.closingFloat,
                    onValueChange = onFloatChange,
                    label = { Text("Closing Cash Float (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("Rp ") },
                    enabled = !state.isLoading,
                )
            }

            state.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                Button(
                    onClick = onCloseShift,
                    enabled = !state.isLoading && state.shift != null && state.openBills.isEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        Text("Close Day")
                    }
                }
            }

            // DEFECT-016: shifts other than the current one that are still OPEN — most often
            // left behind by another device. Surfaced here rather than hidden, so an owner
            // always has a path to close them (and, transitively, so any bill still attached to
            // one is never permanently unreachable — see OtherOpenShiftCard below).
            if (state.otherOpenShifts.isNotEmpty()) {
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text(
                        text = "Other Open Shifts Detected (${state.otherOpenShifts.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Likely opened by another device. Close each one to include it in reporting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.otherOpenShifts, key = { it.shift.id }) { row ->
                    OtherOpenShiftCard(
                        row = row,
                        onFloatChange = { value -> onOtherShiftFloatChange(row.shift.id, value) },
                        onClose = { onCloseOtherShift(row.shift.id) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun OtherOpenShiftCard(
    row: ShiftCloseViewModel.OtherOpenShift,
    onFloatChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Opened: ${DateUtil.toDisplayString(row.shift.openedAt)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            SummaryRow(label = "Revenue", value = CurrencyFormatter.format(row.revenue))
            SummaryRow(label = "Expenses", value = CurrencyFormatter.format(row.expenses))

            if (row.openBillCount > 0) {
                Text(
                    text = "${row.openBillCount} open bill(s) must be resolved first — check Orders",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                OutlinedTextField(
                    value = row.closingFloat,
                    onValueChange = onFloatChange,
                    label = { Text("Closing Cash Float (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("Rp ") },
                    enabled = !row.isClosing,
                )
                row.error?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = onClose,
                    enabled = !row.isClosing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (row.isClosing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Close This Shift")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
