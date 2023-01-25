package com.paypal.android.nativepayments

import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.checkout.error.ErrorInfo

class PayPalNativeCheckoutError(
    code: Int,
    errorDescription: String,
    correlationID: String? = null,
    val errorInfo: ErrorInfo
) : PayPalSDKError(code, errorDescription, correlationID)
