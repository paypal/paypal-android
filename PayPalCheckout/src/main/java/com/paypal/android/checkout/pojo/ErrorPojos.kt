package com.paypal.android.checkout.pojo


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
    val nativeSdkVersion: String
) {
    internal constructor(errorInfo: com.paypal.checkout.error.ErrorInfo) : this(
        error = errorInfo.error,
        reason = errorInfo.reason,
        correlationIds = CorrelationIds(errorInfo.correlationIds),
        orderId = errorInfo.orderId,
        nativeSdkVersion = errorInfo.nativeSdkVersion
    )
}

data class CorrelationIds(

    /**
     * Correlation id associated when checking eligibility.
     */
    val eligibilityDebugID: String? = null,

    /**
     * Correlation id associated when checking eligibility for payment buttons.
     */
    val fundingEligibilityDebugID: String? = null,

    /**
     * Correlation id associated when updating client config.
     */
    val updateClientConfigDebugID: String? = null,

    /**
     * Correlation id associated when upgrading LSAT.
     */
    val lsatUpgradeDebugID: String? = null,

    /**
     * Correlation id associated when fetching user info.
     */
    val fetchPayloadDebugID: String? = null,

    /**
     * Correlation id associated when converting currency info.
     */
    val currencyConversionDebugID: String? = null,

    /**
     * Correlation id associated when finishing the checkout flow.
     */
    val finishCheckoutDebugID: String? = null
) {
    internal constructor(correlationIds: com.paypal.checkout.error.CorrelationIds) : this(
        eligibilityDebugID = correlationIds.eligibilityDebugID,
        fundingEligibilityDebugID = correlationIds.fundingEligibilityDebugID,
        updateClientConfigDebugID = correlationIds.updateClientConfigDebugID,
        lsatUpgradeDebugID = correlationIds.lsatUpgradeDebugID,
        fetchPayloadDebugID = correlationIds.fetchPayloadDebugID,
        currencyConversionDebugID = correlationIds.currencyConversionDebugID,
        finishCheckoutDebugID = correlationIds.finishCheckoutDebugID
    )
}