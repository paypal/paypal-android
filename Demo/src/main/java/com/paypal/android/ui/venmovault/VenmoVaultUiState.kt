package com.paypal.android.ui.venmovault

import com.paypal.android.api.model.VenmoPaymentToken
import com.paypal.android.api.model.VenmoSetupToken
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.venmo.VenmoEligibilityResult
import com.paypal.android.venmo.VenmoFinishVaultResult

data class VenmoVaultUiState(
    val checkEligibilityState: ActionState<VenmoEligibilityResult, Exception> = ActionState.Idle,
    val createSetupTokenState: ActionState<VenmoSetupToken, Exception> = ActionState.Idle,
    val vaultVenmoState: ActionState<VenmoFinishVaultResult.Success, Exception> = ActionState.Idle,
    val createPaymentTokenState: ActionState<VenmoPaymentToken, Exception> = ActionState.Idle,
) {
    val isEligibilityCheckSuccessful: Boolean
        get() {
            val state = checkEligibilityState
            return state is ActionState.Success &&
                state.value is VenmoEligibilityResult.Eligible
        }

    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionState.Success

    val isVaultVenmoSuccessful: Boolean
        get() = vaultVenmoState is ActionState.Success
}
