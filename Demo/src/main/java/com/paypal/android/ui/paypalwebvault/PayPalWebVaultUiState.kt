package com.paypal.android.ui.paypalwebvault

import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.SetupToken
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutVaultResult

data class PayPalWebVaultUiState(
    val isCreateSetupTokenLoading: Boolean = false,
    val vaultCustomerId: String = "",
    val setupToken: SetupToken? = null,
    val isUpdateSetupTokenLoading: Boolean = false,
    val payPalWebCheckoutVaultResult: PayPalWebCheckoutVaultResult? = null,
    val payPalWebCheckoutVaultError: PayPalSDKError? = null,
    val isCreatePaymentTokenLoading: Boolean = false,
    val paymentToken: PayPalPaymentToken? = null,
    val isVaultingCanceled: Boolean = false,
)
