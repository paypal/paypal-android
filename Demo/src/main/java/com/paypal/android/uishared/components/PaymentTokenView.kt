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
import com.paypal.android.api.model.CardPaymentToken

@Composable
fun PaymentTokenView(paymentToken: CardPaymentToken) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Payment Token",
                style = MaterialTheme.typography.titleLarge
            )
            PropertyView(name = "ID", value = paymentToken.id)
            PropertyView(name = "Customer ID", value = paymentToken.customerId)
            PropertyView(name = "Card Brand", value = paymentToken.cardBrand)
            PropertyView(name = "Card Last 4", value = paymentToken.cardLast4)
        }
    }
}
