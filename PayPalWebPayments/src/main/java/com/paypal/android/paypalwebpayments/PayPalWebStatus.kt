package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalWebStatus {

    class VaultError(val error: PayPalSDKError) : PayPalWebStatus()
    class VaultSuccess(val result: PayPalWebVaultResult) : PayPalWebStatus()
    data object VaultCanceled : PayPalWebStatus()

    class UnknownError(val error: Throwable) : PayPalWebStatus()
    data object NoResult : PayPalWebStatus()
}
