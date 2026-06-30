package com.wfx.warungpos.feature.order.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wfx.warungpos.core.util.CurrencyFormatter
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.domain.model.Bill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDestinationSheet(
    openBills: List<Bill>,
    onGrabAndGo: () -> Unit,
    onNewTable: () -> Unit,
    onExistingBill: (billId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showExistingBills by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (!showExistingBills) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("New Order", style = MaterialTheme.typography.titleLarge)
                DestinationRow("Grab & Go", Icons.Default.ShoppingBag, onClick = onGrabAndGo)
                DestinationRow("New Table", Icons.Default.TableBar, onClick = onNewTable)
                if (openBills.isNotEmpty()) {
                    DestinationRow("Existing Bill", Icons.Default.Receipt, onClick = { showExistingBills = true })
                }
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Select Bill", style = MaterialTheme.typography.titleLarge)
                openBills.forEach { bill ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExistingBill(bill.id) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(bill.sessionLabel, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                DateUtil.toDisplayTime(bill.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(CurrencyFormatter.format(bill.grandTotal), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}
