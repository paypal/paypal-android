package com.paypal.android.ui.paypal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.utils.UIConstants

@Composable
fun PayPalCheckoutResultView(orderId: String?, payerId: String?) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Order ID", value = orderId)
        PropertyView(name = "Payer ID", value = payerId)
    }
}
