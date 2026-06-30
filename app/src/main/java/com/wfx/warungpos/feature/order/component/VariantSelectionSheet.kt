package com.wfx.warungpos.feature.order.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.core.common.VariantSelectionType
import com.wfx.warungpos.core.util.CurrencyFormatter
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.model.VariantGroup
import com.wfx.warungpos.domain.model.VariantOption
import com.wfx.warungpos.domain.model.VariantSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantSelectionSheet(
    menuItem: MenuItem,
    groups: List<VariantGroup>,
    optionsByGroup: Map<String, List<VariantOption>>,
    onConfirm: (List<VariantSelection>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selections by remember { mutableStateOf(mapOf<String, Set<String>>()) }

    val allRequiredSatisfied = groups.filter { it.isRequired }
        .all { g -> selections[g.id]?.isNotEmpty() == true }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(menuItem.name, style = MaterialTheme.typography.titleLarge)
            Text(
                CurrencyFormatter.format(menuItem.basePrice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            groups.forEach { group ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = group.name + if (group.isRequired) " *" else " (optional)",
                    style = MaterialTheme.typography.titleSmall,
                )
                val options = optionsByGroup[group.id].orEmpty()
                val selectedInGroup = selections[group.id].orEmpty()

                if (group.selectionType == VariantSelectionType.SINGLE) {
                    options.forEach { option ->
                        val selected = selectedInGroup.contains(option.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    onClick = { selections = selections + (group.id to setOf(option.id)) },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { selections = selections + (group.id to setOf(option.id)) },
                                )
                                Text(option.name, modifier = Modifier.padding(start = 8.dp))
                            }
                            Text(formatDelta(option.priceDelta), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    options.forEach { option ->
                        val checked = selectedInGroup.contains(option.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = checked,
                                    onValueChange = { isChecked ->
                                        selections = selections + (group.id to if (isChecked) selectedInGroup + option.id else selectedInGroup - option.id)
                                    },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selections = selections + (group.id to if (isChecked) selectedInGroup + option.id else selectedInGroup - option.id)
                                    },
                                )
                                Text(option.name, modifier = Modifier.padding(start = 8.dp))
                            }
                            Text(formatDelta(option.priceDelta), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Button(
                onClick = {
                    val result = groups.flatMap { g ->
                        val opts = optionsByGroup[g.id].orEmpty()
                        selections[g.id].orEmpty().mapNotNull { optId -> opts.find { it.id == optId } }
                            .map { opt ->
                                VariantSelection(
                                    groupId = g.id,
                                    groupName = g.name,
                                    optionId = opt.id,
                                    optionName = opt.name,
                                    priceDelta = opt.priceDelta,
                                )
                            }
                    }
                    onConfirm(result)
                },
                enabled = allRequiredSatisfied,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) { Text("Add to Order") }
        }
    }
}

private fun formatDelta(delta: Long): String = when {
    delta > 0 -> "+${CurrencyFormatter.format(delta)}"
    delta < 0 -> "-${CurrencyFormatter.format(-delta)}"
    else -> "Free"
}
