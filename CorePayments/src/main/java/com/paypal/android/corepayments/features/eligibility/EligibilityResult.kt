package com.paypal.android.corepayments.features.eligibility

import com.paypal.android.corepayments.PayPalSDKError

sealed class EligibilityResult {

    data class Success(val isVenmoEligible: Boolean) : EligibilityResult()
    data class Failure(val error: PayPalSDKError) : EligibilityResult()
}
