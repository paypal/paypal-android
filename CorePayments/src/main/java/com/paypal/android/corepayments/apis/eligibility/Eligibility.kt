package com.paypal.android.corepayments.apis.eligibility

internal data class Eligibility(
    val isVenmoEligible: Boolean,
    val isPaypalEligible: Boolean,
    val isPaypalCreditEligible: Boolean,
    val isPayLaterEligible: Boolean,
    val isCreditCardEligible: Boolean
)
