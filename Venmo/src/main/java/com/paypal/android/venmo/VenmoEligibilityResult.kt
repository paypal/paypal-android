package com.paypal.android.venmo

import com.paypal.android.corepayments.PayPalSDKError

sealed class VenmoEligibilityResult {
    data object Eligible : VenmoEligibilityResult()

    data class Ineligible(val reason: String) : VenmoEligibilityResult()

    data class Error(val error: PayPalSDKError) : VenmoEligibilityResult()
}
