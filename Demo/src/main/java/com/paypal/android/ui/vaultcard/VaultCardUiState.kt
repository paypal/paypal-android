package com.paypal.android.ui.vaultcard

import com.paypal.android.api.model.PaymentToken
import com.paypal.android.api.model.SetupToken
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.uishared.state.ActionButtonState

data class VaultCardUiState(
    val createSetupTokenState: ActionButtonState<SetupToken, Exception> = ActionButtonState.Ready,
    val paymentToken: PaymentToken? = null,
    val isUpdateSetupTokenLoading: Boolean = false,
    val isCreatePaymentTokenLoading: Boolean = false,
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val cardVaultResult: CardVaultResult? = null
) {
    val isCreateSetupTokenSuccessful: Boolean
        get() = createSetupTokenState is ActionButtonState.Success
}
