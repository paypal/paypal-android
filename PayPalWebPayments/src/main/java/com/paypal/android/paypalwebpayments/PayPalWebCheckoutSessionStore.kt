package com.paypal.android.paypalwebpayments

private const val KEY_AUTH_STATE = "PayPal.AUTH_STATE"

internal class PayPalWebCheckoutSessionStore {
    private val properties: MutableMap<String, String?> = mutableMapOf()

    var authState: String?
        get() = properties[KEY_AUTH_STATE]
        set(value) {
            properties[KEY_AUTH_STATE] = value
        }

    fun clear() = properties.clear()
}
