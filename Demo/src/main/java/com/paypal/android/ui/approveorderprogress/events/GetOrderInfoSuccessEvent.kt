package com.paypal.android.ui.approveorderprogress.events

import androidx.compose.runtime.Composable
import com.paypal.android.api.model.Order
import com.paypal.android.ui.approveorderprogress.ComposableEvent
import com.paypal.android.ui.approveorderprogress.composables.GetOrderInfoView

class GetOrderInfoSuccessEvent(private val order: Order): ComposableEvent {

    @Composable
    override fun AsComposable() {
        GetOrderInfoView(order = order)
    }
}