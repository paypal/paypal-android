package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.utils.UIConstants

@Composable
fun PayPalPaymentTokenView(paymentToken: PayPalPaymentToken) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "ID", value = paymentToken.id)
        PropertyView(name = "Customer ID", value = paymentToken.customerId)
    }
}
