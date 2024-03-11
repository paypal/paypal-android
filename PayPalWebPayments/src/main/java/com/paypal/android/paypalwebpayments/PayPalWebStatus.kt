package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

internal sealed class PayPalWebStatus {

    class CheckoutError(val error: PayPalSDKError, val orderId: String?) : PayPalWebStatus()
    class CheckoutSuccess(val result: PayPalWebCheckoutResult) : PayPalWebStatus()
    class CheckoutCanceled(val orderId: String?) : PayPalWebStatus()

    class VaultError(val error: PayPalSDKError) : PayPalWebStatus()
    class VaultSuccess(val result: PayPalWebVaultResult) : PayPalWebStatus()
    object VaultCanceled : PayPalWebStatus()
}
