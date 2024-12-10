package com.paypal.android.ui.vaultcard

import com.paypal.android.api.model.CardPaymentToken
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.ui.approveorder.SetupTokenInfo
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.uishared.state.ActionState

data class VaultCardUiState(
    val createSetupTokenState: ActionState<CardSetupToken, Exception> = ActionState.Idle,
    val updateSetupTokenState: ActionState<SetupTokenInfo, Exception> = ActionState.Idle,
    val createPaymentTokenState: ActionState<CardPaymentToken, Exception> = ActionState.Idle,
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val scaOption: SCA = SCA.SCA_WHEN_REQUIRED,
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionState.Success

    val isVaultCardSuccessful: Boolean
        get() = updateSetupTokenState is ActionState.Success
}
