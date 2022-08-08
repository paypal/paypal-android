package com.paypal.android.checkout

import com.paypal.android.core.PayPalSDKError
import com.paypal.checkout.error.ErrorInfo

class PayPalCheckoutError(code: Int, errorDescription: String, correlationID: String? = null, private val errorInfo: ErrorInfo) :
    PayPalSDKError(code, errorDescription, correlationID)
