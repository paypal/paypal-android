package com.paypal.android.card

import com.paypal.android.card.model.CardResult
import com.paypal.android.core.PayPalSDKError

/**
 * Implement this callback to receive results from [CardClient].
 */
interface ApproveOrderListener {

    /**
     * Called when the order is approved.
     * @param result [CardResult] with order information.
     */
    fun onApproveOrderSuccess(result: CardResult)

    /**
     * Called when the approval fails.
     * @param error [PayPalSDKError] explaining the reason for failure.
     */
    fun onApproveOrderFailure(error: PayPalSDKError)

    /**
     * Called when user cancels the flow.
     */
    fun onApproveOrderCanceled()

    /**
     * Called when the 3DS challenge will launch.
     */
    fun onApproveOrderThreeDSecureWillLaunch()

    /**
     * Called when the 3DS challenge has finished.
     */
    fun onApproveOrderThreeDSecureDidFinish()
}
