package com.paypal.android.ui.paypalwebvault

import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutVaultResult

data class PayPalWebVaultUiState(
    val isCreateSetupTokenLoading: Boolean = false,
    val vaultCustomerId: String = "",
    val setupToken: PayPalSetupToken? = null,
    val isVaultPayPalLoading: Boolean = false,
    val payPalWebCheckoutVaultResult: PayPalWebCheckoutVaultResult? = null,
    val payPalWebCheckoutVaultError: PayPalSDKError? = null,
    val isCreatePaymentTokenLoading: Boolean = false,
    val paymentToken: PayPalPaymentToken? = null,
    val isVaultingCanceled: Boolean = false,
)
