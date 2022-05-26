package com.paypal.android.card.model

import com.paypal.android.card.threedsecure.ThreeDSecureResult
import com.paypal.android.core.containsKey
import com.paypal.android.core.optNullableString
import org.json.JSONObject

data class AuthenticationResult(
    val liabilityShift: String?,
    val threeDSecure: ThreeDSecureResult? = null
) {
    internal constructor(json: JSONObject) : this(
        json.optNullableString("liability_shift"),
        if (json.containsKey("three_d_secure")) ThreeDSecureResult(json.getJSONObject("three_d_secure"))
        else null
    )
}
