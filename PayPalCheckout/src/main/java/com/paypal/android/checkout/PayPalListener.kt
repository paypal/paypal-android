package com.paypal.android.checkout

import com.paypal.android.core.PayPalSDKError

interface PayPalListener {
    fun onPayPalSuccess(result: PayPalCheckoutResult)
    fun onPayPalFailure(error: PayPalSDKError)
    fun onPayPalCanceled()
}
