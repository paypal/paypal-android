package com.paypal.android.cardpayments.model

import com.paypal.android.corepayments.PaymentsJSON
import org.json.JSONObject

internal data class PaymentSource(
    val lastDigits: String,
    val brand: String,
    val type: String? = null,
    val authenticationResult: AuthenticationResult? = null
) {

    companion object {
        const val KEY_LAST_DIGITS = "last_digits"
        const val KEY_BRAND = "brand"
        const val KEY_TYPE = "type"
        const val KEY_AUTHENTICATION_RESULT = "authentication_result"
    }

    internal constructor(json: PaymentsJSON) : this(
        json.getString(KEY_LAST_DIGITS),
        json.getString(KEY_BRAND),
        json.optString(KEY_TYPE),
        json.optMapObject(KEY_AUTHENTICATION_RESULT) { AuthenticationResult(it) }
    )

    fun toJSON(): JSONObject {
        return JSONObject()
            .put(KEY_LAST_DIGITS, lastDigits)
            .put(KEY_BRAND, brand)
            .putOpt(KEY_TYPE, type)
            .putOpt(KEY_AUTHENTICATION_RESULT, authenticationResult?.toJSON())
    }
}
