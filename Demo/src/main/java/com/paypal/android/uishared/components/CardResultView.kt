package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.ui.approveorder.OrderInfo
import com.paypal.android.utils.UIConstants

@Composable
fun CardResultView(result: OrderInfo) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Order ID", value = result.orderId)
        PropertyView(name = "Order Status", value = result.status)
        val didAttemptText = if (result.didAttemptThreeDSecureAuthentication) "YES" else "NO"
        PropertyView(name = "Did Attempt 3DS Authentication", value = didAttemptText)
    }
}

@Preview
@Composable
fun CardResultViewWith3DSAuth() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            val result = OrderInfo(
                orderId = "fake-order-id",
                status = "fake-status",
                didAttemptThreeDSecureAuthentication = true
            )
            CardResultView(result)
        }
    }
}

@Preview
@Composable
fun CardResultViewWithout3DSAuth() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            val result = OrderInfo(
                orderId = "fake-order-id",
                status = "fake-status",
                didAttemptThreeDSecureAuthentication = false
            )
            CardResultView(result)
        }
    }
}
