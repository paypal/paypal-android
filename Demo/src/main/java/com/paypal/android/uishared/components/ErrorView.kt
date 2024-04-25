package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.utils.UIConstants

@Composable
fun ErrorView(error: Exception) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        if (error is PayPalSDKError) {
            PropertyView(name = "Error Code", value = "${error.code}")
            PropertyView(name = "Error Description", value = error.errorDescription)
            PropertyView(name = "Correlation ID", value = error.correlationId)
        } else {
            PropertyView(name = "Message", value = error.message)
        }
    }
}

@Preview
@Composable
fun ErrorViewActionColumnPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
                ErrorView(error = java.lang.Exception("Fake Exception"))
        }
    }
}
