package com.paypal.android.corepayments.features.eligibility

import com.paypal.android.corepayments.OrderIntent

/**
 * @property [intent] An order intent used in the request for eligibility.
 * @property [currencyCode] A currency code used in the request for eligibility.
 */
data class EligibilityRequest(
    val intent: OrderIntent,
    val currencyCode: String
)
