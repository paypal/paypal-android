package com.paypal.android.core

import org.json.JSONObject

data class AnalyticsEventData(
    val appID: String,
    val appName: String,
    val eventName: String
) {
    companion object {
        const val KEY_APP_ID = "app_id"
        const val KEY_APP_NAME = "app_name"
        const val KEY_EVENT_NAME = "event_name"

        const val KEY_EVENT_PARAMETERS = "event_params"
        const val KEY_EVENTS = "events"
    }

    fun toJSON(): JSONObject {
        val eventParams = JSONObject()
            .put(KEY_APP_ID, appID)
            .put(KEY_APP_NAME, appName)
            .put(KEY_EVENT_NAME, eventName)

        val midLevel = JSONObject()
            .put(KEY_EVENT_PARAMETERS, eventParams)

        return JSONObject()
            .put(KEY_EVENTS, midLevel)
    }
}

//data class PaymentSource2(
//    val lastDigits: String,
//    val brand: String,
//    val type: String? = null,
//    val authenticationResult: AuthenticationResult? = null
//) {
//
//    companion object {
//        const val KEY_LAST_DIGITS = "last_digits"
//        const val KEY_BRAND = "brand"
//        const val KEY_TYPE = "type"
//        const val KEY_AUTHENTICATION_RESULT = "authentication_result"
//    }
//
//    internal constructor(json: PaymentsJSON) : this(
//        json.getString(KEY_LAST_DIGITS),
//        json.getString(KEY_BRAND),
//        json.optString(KEY_TYPE),
//        json.optMapObject(KEY_AUTHENTICATION_RESULT) { AuthenticationResult(it) }
//    )
//
//    fun toJSON(): JSONObject {
//        return JSONObject()
//            .put(KEY_LAST_DIGITS, lastDigits)
//            .put(KEY_BRAND, brand)
//            .putOpt(KEY_TYPE, type)
//            .putOpt(KEY_AUTHENTICATION_RESULT, authenticationResult?.toJSON())
//    }
//}