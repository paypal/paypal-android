package com.paypal.android.ui.approveorderprogress.events

import androidx.compose.runtime.Composable
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.ui.approveorderprogress.ComposableEvent
import com.paypal.android.ui.approveorderprogress.composables.CardResultView

class CardResultSuccessEvent(private val cardResult: CardResult) : ComposableEvent {

    @Composable
    override fun AsComposable() {
        CardResultView(cardResult)
    }
}

