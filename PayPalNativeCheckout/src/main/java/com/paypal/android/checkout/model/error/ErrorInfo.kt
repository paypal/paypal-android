package com.paypal.android.checkout.model.error

import com.paypal.android.checkout.model.CorrelationIds

data class ErrorInfo(

    /**
     * [Throwable] that caused the error.
     */
    val error: Throwable,

    /**
     * Reason for the error.
     */
    val reason: String,

    /**
     * Correlation IDs for the checkout session. These are used for debugging purposes.
     */
    val correlationIds: CorrelationIds,

    /**
     * ID of the order.
     */
    val orderId: String?,

    /**
     * Version of the PayPal Checkout SDK.
     */
    val nativeSdkVersion: String? = null
) {
    internal constructor(errorInfo: com.paypal.checkout.error.ErrorInfo) : this(
        error = errorInfo.error,
        reason = errorInfo.reason,
        correlationIds = CorrelationIds(errorInfo.correlationIds),
        orderId = errorInfo.orderId,
        nativeSdkVersion = errorInfo.nativeSdkVersion
    )
}