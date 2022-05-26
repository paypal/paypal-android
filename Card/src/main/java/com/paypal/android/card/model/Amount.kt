package com.paypal.android.card.model

import org.json.JSONObject

data class Amount(
    val currencyCode: String?,
    val value: String?
) {
    internal constructor(json: JSONObject) : this(
        json.getString("currency_code"),
        json.getString("value")
    )
}
