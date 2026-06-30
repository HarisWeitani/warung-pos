package com.wfx.warungpos.feature.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.wfx.warungpos.core.common.ExpenseCategory
import com.wfx.warungpos.core.util.CurrencyFormatter
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.domain.model.Expense

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseLogScreen(
    state: ExpenseLogUiState,
    note: String,
    onNoteChange: (String) -> Unit,
    onShowAdd: () -> Unit,
    onDismissSheet: () -> Unit,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Expenses") }) },
        floatingActionButton = {
            if (state.openShift != null) {
                FloatingActionButton(onClick = onShowAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add expense")
                }
            }
        },
    ) { padding ->
        if (state.expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.openShift == null) "No open shift" else "No expenses recorded",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(state.expenses, key = { it.id }) { expense ->
                    ExpenseRow(expense)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (state.isAddSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismissSheet,
            sheetState = sheetState,
        ) {
            AddExpenseSheet(
                state = state,
                note = note,
                onNoteChange = onNoteChange,
                onCategoryChange = onCategoryChange,
                onAmountChange = onAmountChange,
                onSave = onSave,
                onDismiss = onDismissSheet,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseSheet(
    state: ExpenseLogUiState,
    note: String,
    onNoteChange: (String) -> Unit,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Add Expense", style = MaterialTheme.typography.titleLarge)

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = state.newCategory.name.lowercase().replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ExpenseCategory.entries.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            onCategoryChange(cat)
                            expanded = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.newAmount,
            onValueChange = onAmountChange,
            label = { Text("Amount (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            prefix = { Text("Rp ") },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                enabled = state.newAmount.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = expense.category.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            if (expense.note != null) {
                Text(
                    text = expense.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = DateUtil.toDisplayTime(expense.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = CurrencyFormatter.format(expense.amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
