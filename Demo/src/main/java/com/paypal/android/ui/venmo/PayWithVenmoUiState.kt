package com.paypal.android.ui.venmo

import com.paypal.android.api.model.Order
import com.paypal.android.uishared.state.ActionState

data class PayWithVenmoUiState(
    val createOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val payWithVenmoState: ActionState<*, Exception> = ActionState.Idle,
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionState.Success
}

