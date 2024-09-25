package com.paypal.android.ui.paypalwebvault

import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutAuthResult
import com.paypal.android.paypalwebpayments.PayPalWebVaultAuthResult
import com.paypal.android.paypalwebpayments.PayPalWebVaultResult
import com.paypal.android.uishared.state.ActionState

data class PayPalWebVaultUiState(
    val createSetupTokenState: ActionState<PayPalSetupToken, Exception> = ActionState.Idle,
    val authChallengeState: ActionState<PayPalWebVaultAuthResult.Success, Exception> = ActionState.Idle,
    val createPaymentTokenState: ActionState<PayPalPaymentToken, Exception> = ActionState.Idle,
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionState.Success

    val isAuthSuccessful: Boolean
        get() = authChallengeState is ActionState.Success
}
