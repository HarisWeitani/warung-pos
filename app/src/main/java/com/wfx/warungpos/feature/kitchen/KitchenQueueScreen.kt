package com.wfx.warungpos.feature.kitchen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.core.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenQueueScreen(
    state: KitchenQueueUiState,
    onMarkDone: (orderItemId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Kitchen Queue") }) },
    ) { padding ->
        if (state.rows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No unfulfilled items. All caught up!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(contentPadding = padding, modifier = Modifier.padding(horizontal = 16.dp)) {
                items(state.rows, key = { it.orderItem.id }) { row ->
                    KitchenQueueRowCard(row = row, onMarkDone = { onMarkDone(row.orderItem.id) })
                }
            }
        }
    }
}

@Composable
private fun KitchenQueueRowCard(row: KitchenQueueRow, onMarkDone: () -> Unit) {
    val item = row.orderItem
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.sessionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${item.quantity}× ${item.nameSnapshot}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (item.selectedVariants.isNotEmpty()) {
                    Text(
                        text = item.selectedVariants.joinToString(", ") { it.optionName },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = DateUtil.toDisplayTime(item.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onMarkDone) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text(" Done", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}
