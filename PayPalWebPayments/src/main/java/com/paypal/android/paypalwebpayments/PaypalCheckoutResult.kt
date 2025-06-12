package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

/**
 * Result for PayPal checkout operations (both web and app switch flows)
 */
sealed class PaypalCheckoutResult {
    data class Success(val orderId: String?, val payerId: String?) : PaypalCheckoutResult()
    data class Failure(val error: PayPalSDKError, val orderId: String?) : PaypalCheckoutResult()
    data class Canceled(val orderId: String?) : PaypalCheckoutResult()
    data object NoResult : PaypalCheckoutResult()
}
