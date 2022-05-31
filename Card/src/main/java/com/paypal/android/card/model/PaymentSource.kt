package com.paypal.android.card.model

import com.paypal.android.core.PaymentsJSON

data class PaymentSource(
    val lastDigits: String,
    val brand: String,
    val type: String? = null,
    val authenticationResult: AuthenticationResult? = null
) {
    internal constructor(json: PaymentsJSON) : this(
        json.getString("last_digits"),
        json.getString("brand"),
        json.optString("type"),
        json.optMapObject("authentication_result") { AuthenticationResult(it) }
    )
}
