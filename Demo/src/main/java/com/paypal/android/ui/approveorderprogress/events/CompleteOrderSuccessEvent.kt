package com.paypal.android.ui.approveorderprogress.events

import androidx.compose.runtime.Composable
import com.paypal.android.api.model.Order
import com.paypal.android.ui.approveorderprogress.views.OrderCompleteView
import com.paypal.android.uishared.events.ComposableEvent

class CompleteOrderSuccessEvent(val order: Order) : ComposableEvent {

    @Composable
    override fun AsComposable() {
        OrderCompleteView(order = order)
    }
}
