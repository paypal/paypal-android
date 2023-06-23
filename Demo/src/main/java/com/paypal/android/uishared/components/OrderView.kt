package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.api.model.Order

@Composable
fun OrderView(order: Order) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Order",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = order.id ?: "UNSET",
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Intent",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = order.intent ?: "UNSET",
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = order.status ?: "UNSET",
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Card Last 4",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = order.cardLast4 ?: "UNSET",
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Card Brand",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = order.cardBrand ?: "UNSET",
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Vault Id",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = order.vaultId ?: "UNSET",
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Customer Vault Id",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = order.customerId ?: "UNSET",
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Preview
@Composable
fun OrderPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            OrderView(Order())
        }
    }
}
