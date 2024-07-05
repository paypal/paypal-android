package com.paypal.android.paypalnativepayments

import com.paypal.checkout.error.ErrorInfo
import java.lang.Exception

/**
 * Deprecated. Use PayPalWebPayments module instead
 * Wrapper error type for native checkout error.
 */
@Deprecated("Deprecated. Use PayPalWebPayments module instead")
data class PayPalNativeCheckoutError internal constructor(val errorInfo: ErrorInfo) :
    Exception("PayPal Native Checkout Error")
