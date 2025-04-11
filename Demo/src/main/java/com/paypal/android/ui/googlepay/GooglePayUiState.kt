package com.paypal.android.ui.googlepay

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.corepayments.ApproveGooglePayPaymentResult
import com.paypal.android.uishared.state.ActionState

data class GooglePayUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val createOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val googlePayState: ActionState<ApproveGooglePayPaymentResult.Success, Exception> = ActionState.Idle,
    val completeOrderState: ActionState<Order, Exception> = ActionState.Idle,
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionState.Success

    val isGooglePaySuccessful: Boolean
        get() = googlePayState is ActionState.Success
}
