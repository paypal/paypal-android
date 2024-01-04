package com.paypal.android.ui.vaultcard

import com.paypal.android.api.model.CardPaymentToken
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.uishared.state.ActionButtonState

data class VaultCardUiState(
    val createSetupTokenState: ActionButtonState<CardSetupToken, Exception> = ActionButtonState.Ready,
    val vaultCardState: ActionButtonState<CardVaultResult, Exception> = ActionButtonState.Ready,
    val createPaymentTokenState: ActionButtonState<CardPaymentToken, Exception> = ActionButtonState.Ready,
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionButtonState.Success

    val isVaultCardSuccessful: Boolean
        get() = vaultCardState is ActionButtonState.Success
}
