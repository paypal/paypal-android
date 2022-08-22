package com.paypal.android.viewmodels

import com.paypal.checkout.error.ErrorInfo

sealed class NativeCheckoutViewState {
    object Initial : NativeCheckoutViewState()
    object GeneratingToken : NativeCheckoutViewState()
    class TokenGenerated(val token: String) : NativeCheckoutViewState()
    class OrderCreated(val orderId: String) : NativeCheckoutViewState()
    object OrderPatched : NativeCheckoutViewState()
    object CheckoutInit : NativeCheckoutViewState()
    object CheckoutStart : NativeCheckoutViewState()
    object CheckoutCancelled : NativeCheckoutViewState()
    class CheckoutError(val message: String? = null, val error: ErrorInfo? = null) : NativeCheckoutViewState()
    data class CheckoutComplete(
        val payerId: String?,
        val orderId: String?,
        val paymentId: String?,
        val billingToken: String? = null,
    ) : NativeCheckoutViewState()
}
