package com.paypal.android.corepayments

import org.json.JSONArray
import org.json.JSONObject

data class GooglePayConfig(
    val isEligible: Boolean,
    val allowedPaymentMethods: JSONArray,
    val merchantInfo: JSONObject,
)
