package com.paypal.android.ui.venmovault

import com.paypal.android.api.model.VenmoPaymentToken
import com.paypal.android.api.model.VenmoSetupToken
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.venmo.VenmoFinishVaultResult

data class VenmoVaultUiState(
    val createSetupTokenState: ActionState<VenmoSetupToken, Exception> = ActionState.Idle,
    val vaultVenmoState: ActionState<VenmoFinishVaultResult.Success, Exception> = ActionState.Idle,
    val createPaymentTokenState: ActionState<VenmoPaymentToken, Exception> = ActionState.Idle,
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionState.Success

    val isVaultVenmoSuccessful: Boolean
        get() = vaultVenmoState is ActionState.Success
}
