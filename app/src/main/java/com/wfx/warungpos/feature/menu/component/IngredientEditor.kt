package com.wfx.warungpos.feature.menu.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.wfx.warungpos.domain.model.MenuItemIngredient
import com.wfx.warungpos.domain.model.StockItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientEditor(
    ingredient: MenuItemIngredient,
    stockItems: List<StockItem>,
    onUpdate: (oldStockItemId: String, MenuItemIngredient) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = stockItems.firstOrNull { it.id == ingredient.stockItemId }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.weight(1.4f)) {
            OutlinedTextField(
                value = selected?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Ingredient") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                stockItems.forEach { stockItem ->
                    DropdownMenuItem(
                        text = { Text(stockItem.name) },
                        onClick = {
                            onUpdate(ingredient.stockItemId, ingredient.copy(stockItemId = stockItem.id))
                            expanded = false
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = formatQty(ingredient.qtyPerServing),
            onValueChange = { value ->
                val qty = value.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                onUpdate(ingredient.stockItemId, ingredient.copy(qtyPerServing = qty))
            },
            label = { Text("Qty/serving${selected?.let { " (${it.unit})" } ?: ""}") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Remove ingredient", tint = MaterialTheme.colorScheme.error)
        }
    }
}

private fun formatQty(qty: Double): String =
    if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()
