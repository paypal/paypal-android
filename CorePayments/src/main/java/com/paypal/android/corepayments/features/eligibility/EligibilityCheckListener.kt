package com.paypal.android.corepayments.features.eligibility

import com.paypal.android.corepayments.PayPalSDKError

/**
 * Implement this interface to receive results from calls to [EligibilityClient.check].
 */
interface EligibilityCheckListener {

    /**
     * Called when [EligibilityClient.check] succeeds.
     *
     * @param result The [EligibilityResult].
     */
    fun onCheckEligibilitySuccess(result: EligibilityResult)

    /**
     * Called when [EligibilityClient.check] fails.
     *
     * @param error An error that explains what went wrong.
     */
    fun onCheckEligibilityFailure(error: PayPalSDKError)
}
