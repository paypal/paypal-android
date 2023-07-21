package com.paypal.android.ui.approveorderprogress.events

import androidx.compose.runtime.Composable
import com.paypal.android.api.model.Order
import com.paypal.android.cardpayments.VaultResult
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.ui.approveorderprogress.views.ApproveOrderSuccessView
import com.paypal.android.ui.approveorderprogress.views.GetOrderView
import com.paypal.android.ui.approveorderprogress.views.MessageView
import com.paypal.android.ui.approveorderprogress.views.OrderCompleteView
import com.paypal.android.ui.approveorderprogress.views.VaultSuccessView
import com.paypal.android.uishared.events.ComposableEvent

sealed class ApproveOrderEvent : ComposableEvent {

    class Message(private val message: String) : ApproveOrderEvent() {
        @Composable
        override fun AsComposable() {
            MessageView(message = message)
        }
    }

    class ApproveSuccess(private val cardResult: CardResult) : ApproveOrderEvent() {
        @Composable
        override fun AsComposable() {
            ApproveOrderSuccessView(cardResult = cardResult)
        }
    }

    class GetOrder(private val order: Order) : ApproveOrderEvent() {
        @Composable
        override fun AsComposable() {
            GetOrderView(order = order)
        }
    }

    class OrderComplete(private val order: Order) : ApproveOrderEvent() {
        @Composable
        override fun AsComposable() {
            OrderCompleteView(order = order)
        }
    }

    class VaultSuccess(private val result: VaultResult) : ApproveOrderEvent() {
        @Composable
        override fun AsComposable() {
            VaultSuccessView(result = result)
        }
    }
}
