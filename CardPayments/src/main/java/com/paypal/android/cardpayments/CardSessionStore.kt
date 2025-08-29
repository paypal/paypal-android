package com.paypal.android.cardpayments

private const val KEY_AUTH_STATE = "Card.AUTH_STATE"

internal class CardSessionStore {
    private val properties: MutableMap<String, String?> = mutableMapOf()

    var authState: String?
        get() = properties[KEY_AUTH_STATE]
        set(value) {
            properties[KEY_AUTH_STATE] = value
        }

    fun clear() = properties.clear()
}
