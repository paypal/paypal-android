package com.paypal.android.checkout

import com.paypal.android.core.PayPalSDKError
import com.paypal.checkout.error.ErrorInfo

class PayPalNativeCheckoutError(code: Int, errorDescription: String, correlationID: String? = null, val errorInfo: ErrorInfo) :
    PayPalSDKError(code, errorDescription, correlationID)
