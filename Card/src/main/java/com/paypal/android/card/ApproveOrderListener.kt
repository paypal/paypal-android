package com.paypal.android.card

import androidx.annotation.MainThread
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
    @MainThread
    fun onApproveOrderSuccess(result: CardResult)

    /**
     * Called when the approval fails.
     * @param error [PayPalSDKError] explaining the reason for failure.
     */
    @MainThread
    fun onApproveOrderFailure(error: PayPalSDKError)

    /**
     * Called when user cancels the flow.
     */
    @MainThread
    fun onApproveOrderCanceled()

    /**
     * Called when the 3DS challenge will launch.
     */
    @MainThread
    fun onApproveOrderThreeDSecureWillLaunch()

    /**
     * Called when the 3DS challenge has finished.
     */
    @MainThread
    fun onApproveOrderThreeDSecureDidFinish()
}
