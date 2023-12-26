package com.paypal.android.ui.paypalweb

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult
import com.paypal.android.uishared.state.ActionButtonState

data class PayPalWebUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val createOrderState: ActionButtonState<Order, Exception> = ActionButtonState.Ready,
    val payPalWebCheckoutState: ActionButtonState<PayPalWebCheckoutResult, PayPalSDKError> = ActionButtonState.Ready,
    val completeOrderState: ActionButtonState<Order, PayPalSDKError> = ActionButtonState.Ready,
    val fundingSource: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionButtonState.Success

    val isPayPalWebCheckoutSuccessful: Boolean
        get() = payPalWebCheckoutState is ActionButtonState.Success
}
