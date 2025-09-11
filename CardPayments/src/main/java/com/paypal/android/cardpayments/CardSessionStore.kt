package com.paypal.android.cardpayments

import com.paypal.android.corepayments.SessionStore

private const val KEY_AUTH_STATE = "Card.AUTH_STATE"

internal class CardSessionStore {
    private val properties = SessionStore()

    var authState: String?
        get() = properties[KEY_AUTH_STATE]
        set(value) {
            properties[KEY_AUTH_STATE] = value
        }

    fun clear() = properties.clear()
    fun restore(base64EncodedJSON: String) = properties.restore(base64EncodedJSON)
    fun toBase64EncodedJSON() = properties.toBase64EncodedJSON()
}
