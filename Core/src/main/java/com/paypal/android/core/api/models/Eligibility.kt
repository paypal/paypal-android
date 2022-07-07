package com.paypal.android.core.api.models

data class Eligibility(
    val isVenmoEligible: Boolean,
    val isPaypalEligible: Boolean,
    val isPaypalCreditEligible: Boolean,
    val isPayLaterEligible: Boolean,
    val isCreditCardEligible: Boolean
)
