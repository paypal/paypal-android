package com.paypal.android.ui.venmocheckout

import com.paypal.android.uishared.state.ActionState

data class VenmoCheckoutUiState(
    // TODO: replace once EligibilityResult is emplemented
//    val checkEligibilityState: ActionState<EligibilityResult, Exception> = ActionState.Idle,
    val checkEligibilityState: ActionState<Void, Exception> = ActionState.Idle,
)
