package com.paypal.android.ui.paypalnative

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutResult
import com.paypal.android.uishared.state.ActionState

data class PayPalNativeUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val createOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val payPalNativeCheckoutState: ActionState<PayPalNativeCheckoutResult, Exception> =
        ActionState.Idle,
    val completeOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val shippingPreference: ShippingPreferenceType = ShippingPreferenceType.GET_FROM_FILE,
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionState.Success

    val isPayPalNativeCheckoutSuccessful: Boolean
        get() = payPalNativeCheckoutState is ActionState.Success
}
