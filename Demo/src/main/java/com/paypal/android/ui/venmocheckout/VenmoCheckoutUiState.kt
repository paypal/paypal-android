package com.paypal.android.ui.venmocheckout

import com.paypal.android.api.model.OrderIntent
import com.paypal.android.corepayments.features.eligibility.EligibilityResult
import com.paypal.android.uishared.state.ActionState

data class VenmoCheckoutUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val checkEligibilityState: ActionState<EligibilityResult, Exception> = ActionState.Idle,
)
