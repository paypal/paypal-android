package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.api.model.Order

@Composable
fun OrderView(order: Order, title: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            PropertyView(name = "Order", value = order.id)
            PropertyView(name = "Intent", value = order.intent)
            PropertyView(name = "Status", value = order.status)
            PropertyView(name = "Card Last 4", value = order.cardLast4)
            PropertyView(name = "Card Brand", value = order.cardBrand)
            PropertyView(name = "Vault Id", value = order.vaultId)
            PropertyView(name = "Customer Vault Id", value = order.customerId)
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Preview
@Composable
fun OrderPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            OrderView(Order(), "Sample Title")
        }
    }
}
