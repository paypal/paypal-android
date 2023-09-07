package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paypal.android.corepayments.PayPalSDKError

@Composable
fun PayPalSDKErrorView(error: PayPalSDKError) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("PayPal SDK Error")
            PropertyView(name = "Error Code", value = "${error.code}")
            PropertyView(name = "Error Description", value = error.errorDescription)
            PropertyView(name = "Correlation ID", value = error.correlationId)
        }
    }
}
