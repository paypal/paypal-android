package com.paypal.android.ui.approveorderprogress.views

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
import com.paypal.android.uishared.components.OrderView

@Composable
fun GetOrderView(order: Order) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = "Get Order Info",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        OrderView(order = order)
    }
}

@Preview
@Composable
fun GetOrderViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            GetOrderView(order = Order())
        }
    }
}
