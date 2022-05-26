package com.paypal.android.card.model

import com.paypal.android.core.containsKey
import com.paypal.android.core.optNullableString
import org.json.JSONObject

data class PaymentSource(
    val lastDigits: String,
    val brand: String,
    val type: String? = null,
    val authenticationResult: AuthenticationResult? = null
) {
    internal constructor(json: JSONObject) : this(
        json.getString("last_digits"),
        json.getString("brand"),
        json.optNullableString("type"),
        if (json.containsKey("authentication_result")) AuthenticationResult(json.getJSONObject("authentication_result")) else null
    )
}
