package com.paypal.android.ui.paypalwebvault

import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.paypalwebpayments.PayPalWebVaultResult
import com.paypal.android.uishared.state.ActionState

data class PayPalWebVaultUiState(
    val createSetupTokenState: ActionState<PayPalSetupToken, Exception> = ActionState.Ready,
    val vaultPayPalState: ActionState<PayPalWebVaultResult, Exception> = ActionState.Ready,
    val createPaymentTokenState: ActionState<PayPalPaymentToken, Exception> = ActionState.Ready,
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionState.Success

    val isVaultPayPalSuccessful: Boolean
        get() = vaultPayPalState is ActionState.Success
}
