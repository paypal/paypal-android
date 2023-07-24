package com.paypal.android.ui.approveorderprogress.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.api.model.PaymentToken
import com.paypal.android.uishared.components.Property

@Composable
fun PaymentTokenSuccessView(paymentToken: PaymentToken) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Payment Token Result",
                style = MaterialTheme.typography.titleLarge,
            )
            Property(name = "Payment Token", value = paymentToken.id)
            Property(name = "Customer ID", value = paymentToken.customerId)
            Property(name = "Card Last 4", value = paymentToken.cardLast4)
            Property(name = "Card Brand", value = paymentToken.cardBrand)
        }
        Spacer(modifier = Modifier.size(16.dp))
    }
}

@Preview
@Composable
fun PaymentTokenSuccessViewPreview() {
    val paymentToken = PaymentToken("fake-id", "fake-customer-id", "1234", "fake-brand")
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            PaymentTokenSuccessView(paymentToken)
        }
    }
}
