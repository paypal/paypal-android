package com.paypal.android.cardpayments

import com.paypal.android.corepayments.restoreFromBase64EncodedJSON
import com.paypal.android.corepayments.toBase64EncodedJSON

private const val KEY_AUTH_STATE = "Card.AUTH_STATE"

internal class CardSessionStore {
    private val properties: MutableMap<String, String?> = mutableMapOf()

    var authState: String?
        get() = properties[KEY_AUTH_STATE]
        set(value) {
            properties[KEY_AUTH_STATE] = value
        }

    fun clear() = properties.clear()

    fun restore(base64EncodedJSON: String) =
        properties.restoreFromBase64EncodedJSON(base64EncodedJSON)

    fun toBase64EncodedJSON() = properties.toBase64EncodedJSON()
}
