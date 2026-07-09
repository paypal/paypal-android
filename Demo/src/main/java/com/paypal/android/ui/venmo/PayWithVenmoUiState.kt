package com.paypal.android.ui.venmo

import com.paypal.android.api.model.Order
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.venmo.VenmoEligibilityResult
import com.paypal.android.venmo.VenmoFinishStartResult

data class PayWithVenmoUiState(
    val checkEligibilityState: ActionState<VenmoEligibilityResult, Exception> = ActionState.Idle,
    val createOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val payWithVenmoState: ActionState<VenmoFinishStartResult, Exception> = ActionState.Idle,
    val completeOrderState: ActionState<Order, Exception> = ActionState.Idle
) {
    val isEligibilityCheckSuccessful: Boolean
        get() = checkEligibilityState is ActionState.Success

    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionState.Success

    val isVenmoSuccessful: Boolean
        get() = payWithVenmoState is ActionState.Success
}
