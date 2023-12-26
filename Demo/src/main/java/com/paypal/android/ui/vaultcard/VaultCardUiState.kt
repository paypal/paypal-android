package com.paypal.android.ui.vaultcard

import com.paypal.android.api.model.PaymentToken
import com.paypal.android.api.model.SetupToken
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.uishared.state.ActionButtonState

data class VaultCardUiState(
    val createSetupTokenState: ActionButtonState<SetupToken, Exception> = ActionButtonState.Ready,
    val vaultCardState: ActionButtonState<CardVaultResult, Exception> = ActionButtonState.Ready,
    val createPaymentTokenState: ActionButtonState<PaymentToken, Exception> = ActionButtonState.Ready,
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionButtonState.Success

    val isVaultCardSuccessful: Boolean
        get() = vaultCardState is ActionButtonState.Success
}
