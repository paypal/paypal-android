package com.paypal.android.card.model

import com.paypal.android.card.threedsecure.ThreeDSecureResult
import com.paypal.android.core.PaymentsJSON
import com.paypal.android.core.containsKey
import com.paypal.android.core.optNullableString
import org.json.JSONObject

data class AuthenticationResult(
    val liabilityShift: String?,
    val threeDSecure: ThreeDSecureResult? = null
) {
    internal constructor(json: PaymentsJSON) : this(
        json.optString("liability_shift"),
        json.optMapObject("three_d_secure") { ThreeDSecureResult(it) }
    )
}
