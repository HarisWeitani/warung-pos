package com.wfx.warungpos.feature.menu.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.core.common.VariantSelectionType
import com.wfx.warungpos.domain.model.VariantGroup
import com.wfx.warungpos.domain.model.VariantOption
import com.wfx.warungpos.feature.menu.VariantGroupUi

@Composable
fun VariantGroupEditor(
    groupUi: VariantGroupUi,
    onUpdateGroup: (VariantGroup) -> Unit,
    onDeleteGroup: () -> Unit,
    onAddOption: () -> Unit,
    onUpdateOption: (VariantOption) -> Unit,
    onDeleteOption: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val group = groupUi.group

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = group.name,
                    onValueChange = { onUpdateGroup(group.copy(name = it)) },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDeleteGroup) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete group", tint = MaterialTheme.colorScheme.error)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = group.selectionType == VariantSelectionType.SINGLE,
                    onClick = { onUpdateGroup(group.copy(selectionType = VariantSelectionType.SINGLE)) },
                    label = { Text("Single") },
                )
                FilterChip(
                    selected = group.selectionType == VariantSelectionType.MULTIPLE,
                    onClick = { onUpdateGroup(group.copy(selectionType = VariantSelectionType.MULTIPLE)) },
                    label = { Text("Multiple") },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Required", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = group.isRequired, onCheckedChange = { onUpdateGroup(group.copy(isRequired = it)) })
            }

            HorizontalDivider()

            groupUi.options.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = option.name,
                        onValueChange = { onUpdateOption(option.copy(name = it)) },
                        label = { Text("Option") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = option.priceDelta.toString(),
                        onValueChange = { v ->
                            val delta = v.filter { it.isDigit() || it == '-' }.toLongOrNull() ?: 0L
                            onUpdateOption(option.copy(priceDelta = delta))
                        },
                        label = { Text("+/- Rp") },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f),
                    )
                    IconButton(onClick = { onDeleteOption(option.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete option", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            TextButton(onClick = onAddOption) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add Option")
            }
        }
    }
}
