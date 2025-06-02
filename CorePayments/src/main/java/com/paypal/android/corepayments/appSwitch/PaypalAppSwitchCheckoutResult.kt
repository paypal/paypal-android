package com.paypal.android.corepayments.appSwitch

import com.paypal.android.corepayments.PayPalSDKError

sealed class PaypalAppSwitchCheckoutResult {
    data class Success(val orderId: String?, val payerId: String?) : PaypalAppSwitchCheckoutResult()
    data class Failure(val error: PayPalSDKError, val orderId: String?) :
        PaypalAppSwitchCheckoutResult()

    data class Canceled(val orderId: String?) : PaypalAppSwitchCheckoutResult()
}