package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.utils.UIConstants

@Composable
fun PayPalSDKErrorView(error: PayPalSDKError) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Error Code", value = "${error.code}")
        PropertyView(name = "Error Description", value = error.errorDescription)
        PropertyView(name = "Correlation ID", value = error.correlationId)
    }
}
