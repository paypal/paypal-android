package com.paypal.android.paypalnativepayments

import com.paypal.checkout.error.ErrorInfo
import java.lang.Exception

/**
 * Wrapper error type for native checkout error.
 */
data class PayPalNativeCheckoutError internal constructor(val errorInfo: ErrorInfo) :
    Exception("PayPal Native Checkout Error")
