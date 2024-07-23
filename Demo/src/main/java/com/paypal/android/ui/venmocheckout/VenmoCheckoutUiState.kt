package com.paypal.android.ui.venmocheckout

import com.paypal.android.api.model.OrderIntent
import com.paypal.android.uishared.state.ActionState

data class VenmoCheckoutUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    // TODO: replace with VenmoResult once it's implemented
    val venmoCheckoutState: ActionState<Void, Exception> = ActionState.Idle,
)
