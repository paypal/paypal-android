package com.paypal.android.ui.vaultcard

import com.paypal.android.api.model.PaymentToken
import com.paypal.android.api.model.SetupToken
import com.paypal.android.cardpayments.CardVaultResult

data class VaultCardUiState(
    val setupToken: SetupToken? = null,
    val paymentToken: PaymentToken? = null,
    val isCreateSetupTokenLoading: Boolean = false,
    val isUpdateSetupTokenLoading: Boolean = false,
    val isCreatePaymentTokenLoading: Boolean = false,
    val customerId: String = "",
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val cardVaultResult: CardVaultResult? = null
)
