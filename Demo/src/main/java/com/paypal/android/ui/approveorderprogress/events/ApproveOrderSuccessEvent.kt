package com.paypal.android.ui.approveorderprogress.events

import androidx.compose.runtime.Composable
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.ui.approveorderprogress.views.ApproveOrderSuccessView
import com.paypal.android.uishared.events.ComposableEvent

class ApproveOrderSuccessEvent(private val cardResult: CardResult) : ComposableEvent {

    @Composable
    override fun AsComposable() {
        ApproveOrderSuccessView(cardResult = cardResult)
    }
}
