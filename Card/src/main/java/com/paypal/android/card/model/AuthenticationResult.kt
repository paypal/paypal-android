package com.paypal.android.card.model

import com.paypal.android.card.threedsecure.ThreeDSecureResult
import com.paypal.android.core.PaymentsJSON
import org.json.JSONObject

data class AuthenticationResult(
    val liabilityShift: String?,
    val threeDSecure: ThreeDSecureResult? = null
) {

    companion object {
        const val KEY_LIABILITY_SHIFT = "liability_shift"
        const val KEY_THREE_D_SECURE = "three_d_secure"
    }

    internal constructor(json: PaymentsJSON) : this(
        json.optString(KEY_LIABILITY_SHIFT),
        json.optMapObject(KEY_THREE_D_SECURE) { ThreeDSecureResult(it) }
    )

    fun toJSON(): JSONObject {
        return JSONObject()
            .putOpt(KEY_LIABILITY_SHIFT, liabilityShift)
            .putOpt(KEY_THREE_D_SECURE, threeDSecure?.toJSON())
    }
}
