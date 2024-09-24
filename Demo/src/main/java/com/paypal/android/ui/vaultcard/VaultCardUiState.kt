package com.paypal.android.ui.vaultcard

import com.paypal.android.api.model.CardPaymentToken
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.cardpayments.CardAuthChallenge
import com.paypal.android.cardpayments.CardVaultAuthResult
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.uishared.state.ActionState

data class VaultCardUiState(
    val createSetupTokenState: ActionState<CardSetupToken, Exception> = ActionState.Idle,
    val updateSetupTokenState: ActionState<CardVaultResult.Success, Exception> = ActionState.Idle,
    val authChallengeState: ActionState<CardVaultAuthResult.Success, Exception> = ActionState.Idle,
    val createPaymentTokenState: ActionState<CardPaymentToken, Exception> = ActionState.Idle,
    val refreshSetupTokenState: ActionState<CardSetupToken, Exception> = ActionState.Idle,
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val scaOption: SCA = SCA.SCA_WHEN_REQUIRED,
    val authChallenge: CardAuthChallenge? = null
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionState.Success

    val isVaultCardSuccessful: Boolean
        get() = updateSetupTokenState is ActionState.Success

    val isVaultWith3DSSuccessful: Boolean
        get() = authChallengeState is ActionState.Success
}
