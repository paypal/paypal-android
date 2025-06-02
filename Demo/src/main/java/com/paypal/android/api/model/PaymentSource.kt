package com.paypal.android.api.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class PaymentSource(
    val paypal: PayPalDetails? = null
) : Parcelable {
    companion object {
        fun fromJson(json: String?): PaymentSource? {
            return if (json.isNullOrEmpty()) null else {
                val jsonObject = JSONObject(json)
                fromJson(jsonObject)
            }
        }

        internal fun fromJson(jsonObject: JSONObject): PaymentSource? {
            return jsonObject.optJSONObject("paypal")?.let {
                PayPalDetails.fromJson(it)?.let { details ->
                    PaymentSource(details)
                }
            }
        }
    }
}

@Parcelize
data class PayPalDetails(
    val emailAddress: String? = null,
    val appSwitchEligibility: Boolean? = null
) : Parcelable {
    companion object {
        fun fromJson(json: String?): PayPalDetails? {
            return if (json.isNullOrEmpty()) null else {
                fromJson(JSONObject(json))
            }
        }

        fun fromJson(jsonObject: JSONObject?): PayPalDetails? {
            return jsonObject?.let {
                PayPalDetails(
                    emailAddress = it.optString("emailAddress"),
                    appSwitchEligibility = it.optBoolean("appSwitchEligibility"),
                )
            }
        }
    }
}