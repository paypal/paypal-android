package com.paypal.android.card.threedsecure

import com.paypal.android.core.optNullableString
import org.json.JSONObject

data class ThreeDSecureResult(
    val enrollmentStatus: String? = null,
    val authenticationStatus: String? = null
) {
    internal constructor(json: JSONObject) : this(
        json.optNullableString("enrollment_status"),
        json.optNullableString("authentication_status")
    )
}
