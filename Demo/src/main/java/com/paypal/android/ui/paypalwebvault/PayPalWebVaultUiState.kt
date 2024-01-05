package com.paypal.android.ui.paypalwebvault

import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalWebVaultResult

data class PayPalWebVaultUiState(
    val isCreateSetupTokenLoading: Boolean = false,
    val vaultCustomerId: String = "",
    val setupToken: PayPalSetupToken? = null,
    val isVaultPayPalLoading: Boolean = false,
    val payPalWebVaultResult: PayPalWebVaultResult? = null,
    val payPalWebVaultError: PayPalSDKError? = null,
    val isCreatePaymentTokenLoading: Boolean = false,
    val paymentToken: PayPalPaymentToken? = null,
    val isVaultingCanceled: Boolean = false,
)
