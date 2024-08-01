package com.paypal.android.corepayments.apis.eligibility

internal data class Eligibility(
    val isVenmoEligible: Boolean,
    val isPayPalEligible: Boolean,
    val isPayPalCreditEligible: Boolean,
    val isPayLaterEligible: Boolean,
    val isCreditCardEligible: Boolean
)
