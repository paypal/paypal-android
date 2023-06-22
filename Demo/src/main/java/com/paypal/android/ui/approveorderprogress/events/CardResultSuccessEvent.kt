package com.paypal.android.ui.approveorderprogress.events

import androidx.compose.runtime.Composable
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.ui.approveorderprogress.ApproveOrderEvent
import com.paypal.android.ui.approveorderprogress.composables.CardResultView

class CardResultSuccessEvent(private val cardResult: CardResult) : ApproveOrderEvent {

    @Composable
    override fun AsComposable() {
        CardResultView(cardResult)
    }
}

