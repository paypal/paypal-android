package com.paypal.android.ui.paypalnative

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutResult
import com.paypal.android.uishared.state.ActionButtonState

data class PayPalNativeUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val createOrderState: ActionButtonState<Order, Exception> = ActionButtonState.Ready,
    val payPalNativeCheckoutState: ActionButtonState<PayPalNativeCheckoutResult, PayPalSDKError> =
        ActionButtonState.Ready,
    val completeOrderState: ActionButtonState<Order, Exception> = ActionButtonState.Ready,
    val shippingPreference: ShippingPreferenceType = ShippingPreferenceType.GET_FROM_FILE,
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionButtonState.Success

    val isPayPalNativeCheckoutSuccessful: Boolean
        get() = payPalNativeCheckoutState is ActionButtonState.Success
}
