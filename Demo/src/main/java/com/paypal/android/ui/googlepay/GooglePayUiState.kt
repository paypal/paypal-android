package com.paypal.android.ui.googlepay

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.googlepay.GooglePayFinishStartResult
import com.paypal.android.googlepay.GooglePayStartResult
import com.paypal.android.uishared.state.ActionState

data class GooglePayUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val createOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val googlePayFinishStartState: ActionState<GooglePayFinishStartResult.Success, Exception> = ActionState.Idle,
    val completeOrderState: ActionState<Order, Exception> = ActionState.Idle,
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionState.Success

    val isGooglePayFinished: Boolean
        get() = googlePayFinishStartState is ActionState.Success
}
