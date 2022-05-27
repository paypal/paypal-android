package com.paypal.android.card.model

import org.json.JSONObject

data class Amount(
    val currencyCode: String?,
    val value: String?
) {
    internal constructor(json: JSONObject) : this(
        json.optString("currency_code"),
        json.optString("value")
    )
}
