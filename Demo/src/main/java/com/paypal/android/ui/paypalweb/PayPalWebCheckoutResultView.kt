package com.paypal.android.ui.paypalweb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paypal.android.R
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult
import com.paypal.android.uishared.components.PropertyView

@Composable
fun PayPalWebCheckoutResultView(orderId: String?, payerId: String?) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = stringResource(id = R.string.order_approved),
                style = MaterialTheme.typography.titleLarge
            )
            PropertyView(name = "Order ID", value = orderId)
            PropertyView(name = "Payer ID", value = payerId)
        }
    }
}
