package com.paypal.android.ui.vault

import com.paypal.android.api.model.PaymentToken
import com.paypal.android.cardpayments.VaultResult

data class VaultUiState(
    val setupToken: String = "",
    val paymentToken: PaymentToken? = null,
    val isCreateSetupTokenLoading: Boolean = false,
    val isUpdateSetupTokenLoading: Boolean = false,
    val isCreatePaymentTokenLoading: Boolean = false,
    val customerId: String = "",
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val vaultResult: VaultResult? = null
)