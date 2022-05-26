package com.paypal.android.card.model

import org.json.JSONObject

data class Payee(val emailAddress: String) {
    internal constructor(json: JSONObject) : this(json.getString("email_address"))
}
