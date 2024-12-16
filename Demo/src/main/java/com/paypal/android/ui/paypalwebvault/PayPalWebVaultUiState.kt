package com.paypal.android.ui.paypalwebvault

import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishVaultResult
import com.paypal.android.uishared.state.ActionState

data class PayPalWebVaultUiState(
    val createSetupTokenState: ActionState<PayPalSetupToken, Exception> = ActionState.Idle,
    val vaultPayPalState: ActionState<PayPalWebCheckoutFinishVaultResult.Success, Exception> = ActionState.Idle,
    val createPaymentTokenState: ActionState<PayPalPaymentToken, Exception> = ActionState.Idle,
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionState.Success

    val isVaultPayPalSuccessful: Boolean
        get() = vaultPayPalState is ActionState.Success
}
