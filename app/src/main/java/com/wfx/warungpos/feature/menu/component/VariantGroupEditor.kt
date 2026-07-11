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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // DEFECT-007: every keystroke here round-trips through updateGroup()/updateOption() in the
    // ViewModel, which writes to Room *and then reloads every group and option for this item from
    // Room* before the new value flows back down. Feeding the TextField's `value` straight from
    // that round-tripped `group`/`option` meant fast typing could show a stale or reordered
    // value mid-flight — keystrokes landing before the previous one's DB round trip completed
    // would render against out-of-date text, scrambling or dropping characters. `remember`ing a
    // local buffer keyed on the stable id means what's on screen is always exactly what was
    // typed, independent of how slow or out-of-order the persistence round trip is; the id key
    // still lets the field correctly pick up a genuinely different group/option if the list
    // reshuffles (e.g. after a delete).
    var nameText by remember(group.id) { mutableStateOf(group.name) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        onUpdateGroup(group.copy(name = it))
                    },
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
                // Same local-buffer reasoning as nameText above. The price field additionally
                // used to display `priceDelta.toString()` — a *reformatted* value derived from
                // whatever last parsed — so an in-progress "-" (typing a negative delta) parsed
                // to null, fell back to 0L, and immediately overwrote the "-" the user had just
                // typed before they could type the digit after it. Buffering the raw typed text
                // fixes that too: the field shows exactly what was typed, and only the parsed
                // Long (falling back to 0 for a still-incomplete string like a lone "-") is sent
                // to the ViewModel.
                var optionNameText by remember(option.id) { mutableStateOf(option.name) }
                var priceText by remember(option.id) { mutableStateOf(option.priceDelta.toString()) }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = optionNameText,
                        onValueChange = {
                            optionNameText = it
                            onUpdateOption(option.copy(name = it))
                        },
                        label = { Text("Option") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { v ->
                            val filtered = v.filter { it.isDigit() || it == '-' }
                            priceText = filtered
                            val delta = filtered.toLongOrNull() ?: 0L
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
