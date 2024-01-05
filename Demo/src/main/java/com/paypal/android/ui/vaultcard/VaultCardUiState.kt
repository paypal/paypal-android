package com.paypal.android.ui.vaultcard

import com.paypal.android.api.model.CardPaymentToken
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.cardpayments.CardVaultResult

data class VaultCardUiState(
    val setupToken: CardSetupToken? = null,
    val paymentToken: CardPaymentToken? = null,
    val isCreateSetupTokenLoading: Boolean = false,
    val isUpdateSetupTokenLoading: Boolean = false,
    val isCreatePaymentTokenLoading: Boolean = false,
    val customerId: String = "",
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val cardVaultResult: CardVaultResult? = null
)
