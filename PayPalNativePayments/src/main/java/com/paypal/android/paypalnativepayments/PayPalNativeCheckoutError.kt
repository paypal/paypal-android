package com.paypal.android.paypalnativepayments

import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.checkout.error.ErrorInfo

// TODO: remove this type; let's keep a single PayPalSDKError type
class PayPalNativeCheckoutError(
    code: Int,
    errorDescription: String,
    correlationId: String? = null,
    val errorInfo: ErrorInfo
) : PayPalSDKError(code, errorDescription, correlationId)
