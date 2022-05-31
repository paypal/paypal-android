package com.paypal.android.card.threedsecure

import com.paypal.android.core.PaymentsJSON

data class ThreeDSecureResult(
    val enrollmentStatus: String? = null,
    val authenticationStatus: String? = null
) {
    internal constructor(json: PaymentsJSON) : this(
        json.optString("enrollment_status"),
        json.optString("authentication_status")
    )
}
