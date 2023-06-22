package com.paypal.android.ui.approveorderprogress.events

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.uishared.events.ComposableEvent
import com.paypal.android.ui.approveorderprogress.composables.CardResultView

class CardResultSuccessEvent(private val cardResult: CardResult) : ComposableEvent {

    @Composable
    override fun AsComposable() {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.size(16.dp))
            CardResultView(cardResult)
        }
    }
}

