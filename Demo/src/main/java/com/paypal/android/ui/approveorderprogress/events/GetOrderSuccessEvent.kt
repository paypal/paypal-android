package com.paypal.android.ui.approveorderprogress.events

import androidx.compose.runtime.Composable
import com.paypal.android.api.model.Order
import com.paypal.android.ui.approveorderprogress.views.GetOrderView
import com.paypal.android.uishared.events.ComposableEvent

class GetOrderSuccessEvent(private val order: Order) : ComposableEvent {

    @Composable
    override fun AsComposable() {
        GetOrderView(order = order)
    }
}
