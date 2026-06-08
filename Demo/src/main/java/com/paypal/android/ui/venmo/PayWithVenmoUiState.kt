package com.paypal.android.ui.venmo

import com.paypal.android.api.model.Order
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.venmo.VenmoFinishStartResult

data class PayWithVenmoUiState(
    val createOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val payWithVenmoState: ActionState<VenmoFinishStartResult.Success, Exception> = ActionState.Idle,
    val completeOrderState: ActionState<Order, Exception> = ActionState.Idle
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionState.Success

    val isVenmoSuccessful: Boolean
        get() = payWithVenmoState is ActionState.Success
}

