package com.paypal.android.ui.vaultcard

import com.paypal.android.api.model.CardPaymentToken
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.uishared.state.ActionState

data class VaultCardUiState(
    val createSetupTokenState: ActionState<CardSetupToken, Exception> = ActionState.Ready,
    val vaultCardState: ActionState<CardVaultResult, Exception> = ActionState.Ready,
    val createPaymentTokenState: ActionState<CardPaymentToken, Exception> = ActionState.Ready,
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionState.Success

    val isVaultCardSuccessful: Boolean
        get() = vaultCardState is ActionState.Success
}
