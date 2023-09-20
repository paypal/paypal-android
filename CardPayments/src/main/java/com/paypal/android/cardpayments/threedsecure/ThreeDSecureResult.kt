package com.paypal.android.cardpayments.threedsecure

import com.paypal.android.corepayments.PaymentsJSON
import org.json.JSONObject

internal data class ThreeDSecureResult(
    val enrollmentStatus: String? = null,
    val authenticationStatus: String? = null
) {

    companion object {
        const val KEY_ENROLLMENT_STATUS = "enrollment_status"
        const val KEY_AUTHENTICATION_STATUS = "authentication_status"
    }

    internal constructor(json: PaymentsJSON) : this(
        json.optString(KEY_ENROLLMENT_STATUS),
        json.optString(KEY_AUTHENTICATION_STATUS)
    )

    fun toJSON(): JSONObject {
        return JSONObject()
            .putOpt(KEY_ENROLLMENT_STATUS, enrollmentStatus)
            .putOpt(KEY_AUTHENTICATION_STATUS, authenticationStatus)
    }
}
