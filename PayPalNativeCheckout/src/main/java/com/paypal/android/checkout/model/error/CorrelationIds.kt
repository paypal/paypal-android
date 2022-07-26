package com.paypal.android.checkout.model.error

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