package com.paypal.android.ui.venmocheckout

import com.paypal.android.uishared.state.ActionState

data class VenmoCheckoutUiState(
    // TODO: replace with VenmoResult once it's implemented
    val venmoCheckoutState: ActionState<Void, Exception> = ActionState.Idle,
)
