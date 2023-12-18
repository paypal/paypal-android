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
import com.paypal.android.api.model.Order
import com.paypal.android.utils.UIConstants

@Composable
fun OrderView(order: Order, title: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        title?.let { text ->
            Spacer(modifier = Modifier.size(UIConstants.paddingMedium))
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = UIConstants.paddingMedium)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UIConstants.paddingMedium)
        ) {
            PropertyView(name = "ID", value = order.id)
            order.intent?.let { PropertyView(name = "Intent", value = it) }
            order.status?.let { PropertyView(name = "Status", value = it) }
            order.cardLast4?.let { PropertyView(name = "Card Last 4", value = order.cardLast4) }
            order.cardBrand?.let { PropertyView(name = "Card Brand", value = order.cardBrand) }
            order.vaultId?.let {
                PropertyView(name = "Vault Id / Payment Token", value = order.vaultId)
            }
            order.customerId?.let {
                PropertyView(name = "Customer Vault Id", value = order.customerId)
            }
            Spacer(modifier = Modifier.size(UIConstants.paddingLarge))
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
