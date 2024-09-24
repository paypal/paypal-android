package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.cardpayments.CardApproveOrderResult
import com.paypal.android.utils.UIConstants

@Composable
fun CardResultView(orderId: String, status: String?) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Order ID", value = orderId)
        PropertyView(name = "Order Status", value = status)
    }
}

@Preview
@Composable
fun CardResultViewWith3DSAuth() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            CardResultView(orderId = "fake-order-id", status = "fake-status")
        }
    }
}
