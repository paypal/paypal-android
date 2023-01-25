package com.paypal.android.corepayments.api.models

/**
 *  Class representing eligibility for a set of payment methods
 */
internal data class Eligibility(
    val isVenmoEligible: Boolean,
    val isPaypalEligible: Boolean,
    val isPaypalCreditEligible: Boolean,
    val isPayLaterEligible: Boolean,
    val isCreditCardEligible: Boolean
)
