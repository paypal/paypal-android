package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paypal.android.cardpayments.OrderIntent
import com.paypal.android.ui.WireframeButton

@Composable
fun CompleteOrderForm(
    isLoading: Boolean,
    orderIntent: OrderIntent,
    onSubmit: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Complete Order",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.size(8.dp))
            WireframeButton(
                text = "${orderIntent.name} ORDER",
                isLoading = isLoading,
                onClick = { onSubmit() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
