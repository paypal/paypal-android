package com.paypal.android.checkout

interface PayPalListener {
    fun onPayPalSuccess(result: PayPalCheckoutResult.Success)
    fun onPayPalFailure(failure: PayPalCheckoutResult.Failure)
    fun onPayPalCanceled()
}
