package com.paypal.android.ui.paypalvault

import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.paypalpayments.PayPalUserAction
import com.paypal.android.paypalpayments.PayPalUserIdentity
import com.paypal.android.paypalpayments.PayPalFinishVaultResult
import com.paypal.android.uishared.state.ActionState

data class PayPalVaultUiState(
    val createSetupTokenState: ActionState<PayPalSetupToken, Exception> = ActionState.Idle,
    val userIdentity: PayPalUserIdentity? = null,
    val userAction: PayPalUserAction = PayPalUserAction.SETUP_NOW,
    val vaultPayPalState: ActionState<PayPalFinishVaultResult.Success, Exception> = ActionState.Idle,
    val createPaymentTokenState: ActionState<PayPalPaymentToken, Exception> = ActionState.Idle,
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionState.Success

    val isVaultPayPalSuccessful: Boolean
        get() = vaultPayPalState is ActionState.Success
}
