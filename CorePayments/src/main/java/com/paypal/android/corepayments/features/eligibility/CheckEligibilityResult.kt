package com.paypal.android.corepayments.features.eligibility

import com.paypal.android.corepayments.PayPalSDKError

interface CheckEligibilityResult {
    fun onCheckEligibilitySuccess(result: EligibilityResult)

    fun onCheckEligibilityFailure(error: PayPalSDKError)
}
