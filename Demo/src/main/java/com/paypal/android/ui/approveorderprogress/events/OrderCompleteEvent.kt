package com.paypal.android.ui.approveorderprogress.events

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
import com.paypal.android.api.model.Order
import com.paypal.android.ui.approveorderprogress.ComposableEvent
import com.paypal.android.ui.approveorderprogress.composables.OrderView

class OrderCompleteEvent(val order: Order) : ComposableEvent {

    @Composable
    override fun AsComposable() {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = "Order Complete",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            OrderView(order = order)
        }
    }
}