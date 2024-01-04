package com.paypal.android.ui.paypalwebvault

import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalWebVaultResult
import com.paypal.android.uishared.state.ActionButtonState

data class PayPalWebVaultUiState(
    val createSetupTokenState: ActionButtonState<PayPalSetupToken, Exception> = ActionButtonState.Ready,
    val vaultPayPalState: ActionButtonState<PayPalWebVaultResult, Exception> = ActionButtonState.Ready,
    val createPaymentTokenState: ActionButtonState<PayPalPaymentToken, Exception> = ActionButtonState.Ready,
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = vaultPayPalState is ActionButtonState.Success

    val isVaultPayPalSuccessful: Boolean
        get() = vaultPayPalState is ActionButtonState.Success
}
