package com.paypal.android.ui.approveorderprogress.events

import androidx.compose.runtime.Composable
import com.paypal.android.ui.approveorderprogress.views.MessageView
import com.paypal.android.uishared.events.ComposableEvent

class MessageEvent(private val message: String) : ComposableEvent {

    @Composable
    override fun AsComposable() {
        MessageView(message = message)
    }
}
