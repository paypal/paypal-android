package com.paypal.android.plainclothes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paypal.android.utils.UIConstants

@Composable
fun CheckoutSuccessView(orderId: String = "123") {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier
            .padding(16.dp)
    ) {
        Text(
            text = "Order Complete",
            style = MaterialTheme.typography.displayMedium
        )
        ItemDetails()
        Row {
            Text(
                text = "Order ID",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = orderId,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .weight(1f)
            )
        }
    }
}