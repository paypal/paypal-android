package com.paypal.android.ui.card

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.cardpayments.CardResult
import com.paypal.android.uishared.components.CardResultView

@Composable
fun ApproveOrderSuccessView(cardResult: CardResult) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        CardResultView(cardResult)
    }
}

@Preview
@Composable
fun ApproveOrderSuccessViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            ApproveOrderSuccessView(cardResult = CardResult("fake-order-id"))
        }
    }
}
