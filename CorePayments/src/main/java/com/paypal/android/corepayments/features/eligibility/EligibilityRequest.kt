package com.paypal.android.corepayments.features.eligibility

import com.paypal.android.corepayments.OrderIntent

data class EligibilityRequest(
    val intent: OrderIntent,
    val currencyCode: String
)
