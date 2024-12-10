package com.paypal.android.ui.paypalweb

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.uishared.state.ActionState

data class PayPalWebUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val createOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val payPalWebCheckoutState: ActionState<PayPalWebCheckoutFinishStartResult.Success, Exception> = ActionState.Idle,
    val completeOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val fundingSource: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionState.Success

    val isPayPalWebCheckoutSuccessful: Boolean
        get() = payPalWebCheckoutState is ActionState.Success
}
