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
            Property(name = "Order", value = order.id)
            Property(name = "Intent", value = order.intent)
            Property(name = "Status", value = order.status)
            Property(name = "Card Last 4", value = order.cardLast4)
            Property(name = "Card Brand", value = order.cardBrand)
            Property(name = "Vault Id", value = order.vaultId)
            Property(name = "Customer Vault Id", value = order.customerId)
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun Property(name: String, value: String?) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp)
    )
    Text(
        text = value ?: "UNSET",
        modifier = Modifier.padding(top = 4.dp)
    )
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
