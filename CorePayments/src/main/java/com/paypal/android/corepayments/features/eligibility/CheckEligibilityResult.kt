package com.paypal.android.corepayments.features.eligibility

fun interface CheckEligibilityResult {
    fun onCheckEligibilityResult(result: EligibilityResult)
}