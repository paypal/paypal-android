package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.SessionStore

const val KEY_AUTH_STATE = "auth_state"

class PayPalSessionStore() {
    private val sessionStore = SessionStore()

    internal var authState: String?
        get() = sessionStore.get(KEY_AUTH_STATE)
        set(value) = sessionStore.put(KEY_AUTH_STATE, value)

    internal fun clear() = sessionStore.clear()
    internal fun restore(base64EncodedJSON: String) = sessionStore.restore(base64EncodedJSON)

    internal fun toBase64EncodedJSON() = sessionStore.toBase64EncodedJSON()
}
