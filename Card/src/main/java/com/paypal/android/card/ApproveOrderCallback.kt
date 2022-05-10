package com.paypal.android.card

import com.paypal.android.core.PayPalSDKError

/**
 * Implement this callback to receive results from [CardClient].
 * Recommended for Java Integrations
 */
interface ApproveOrderCallback {

    /**
     * Called when the order is approved.
     * @param result [CardResult] with order information.
     */
    fun success(result: CardResult)

    /**
     * Called when the approval fails.
     * @param error [PayPalSDKError] explaining the reason for failure.
     */
    fun failure(error: PayPalSDKError)

    /**
     * Called when user cancels the flow.
     */
    fun cancelled()

    /**
     * Called when the 3DS challenge will launch
     */
    fun threeDSecureLaunched()
}
