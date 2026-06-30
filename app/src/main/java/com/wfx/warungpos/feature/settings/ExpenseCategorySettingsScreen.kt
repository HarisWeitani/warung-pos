package com.wfx.warungpos.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.core.common.ExpenseCategory

private val CATEGORY_LABELS = mapOf(
    ExpenseCategory.SUPPLIES to ("Supplies" to "Perlengkapan"),
    ExpenseCategory.UTILITIES to ("Utilities" to "Utilitas"),
    ExpenseCategory.SALARY to ("Salary" to "Gaji"),
    ExpenseCategory.RENT to ("Rent" to "Sewa"),
    ExpenseCategory.TRANSPORT to ("Transport" to "Transportasi"),
    ExpenseCategory.OTHER to ("Other" to "Lainnya"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCategorySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Expense Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(ExpenseCategory.entries.toList()) { category ->
                val (en, id) = CATEGORY_LABELS.getValue(category)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(en, style = MaterialTheme.typography.bodyLarge)
                    Text(id, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
        }
    }
}
