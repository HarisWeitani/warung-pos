package com.wfx.warungpos.feature.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.feature.menu.component.VariantGroupEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuItemEditScreen(
    state: MenuItemEditUiState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onSave: () -> Unit,
    onAddVariantGroup: () -> Unit,
    onUpdateGroup: (com.wfx.warungpos.domain.model.VariantGroup) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onAddOption: (String) -> Unit,
    onUpdateOption: (com.wfx.warungpos.domain.model.VariantOption) -> Unit,
    onDeleteOption: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    val isValid = state.name.isNotBlank() && (state.price.toLongOrNull() ?: 0L) > 0 && state.categoryId != null

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (state.itemId != null) "Edit Item" else "New Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(
                        value = state.categories.firstOrNull { it.id == state.categoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        state.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = { onCategoryChange(cat.id); categoryExpanded = false },
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.price,
                    onValueChange = onPriceChange,
                    label = { Text("Base Price (Rp)") },
                    singleLine = true,
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.error?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }

            item {
                Button(
                    onClick = onSave,
                    enabled = isValid && !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.itemId == null) "Save & Continue" else "Save") }
            }

            if (state.itemId != null) {
                item {
                    Text(
                        text = "Variants",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(state.variantGroups, key = { it.group.id }) { groupUi ->
                    VariantGroupEditor(
                        groupUi = groupUi,
                        onUpdateGroup = onUpdateGroup,
                        onDeleteGroup = { onDeleteGroup(groupUi.group.id) },
                        onAddOption = { onAddOption(groupUi.group.id) },
                        onUpdateOption = onUpdateOption,
                        onDeleteOption = onDeleteOption,
                    )
                }

                item {
                    TextButton(onClick = onAddVariantGroup) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add Variant Group")
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
