package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.SessionStore
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState

private const val KEY_AUTH_STATE = "PayPal.AUTH_STATE"

internal class PayPalWebCheckoutSessionStore {
    private val properties = SessionStore()

    var browserSwitchPendingState: BrowserSwitchPendingState? = null

    var authState: String?
        get() = properties[KEY_AUTH_STATE]
        set(value) {
            properties[KEY_AUTH_STATE] = value
        }

    fun clear() = properties.clear()
    fun restore(base64EncodedJSON: String) = properties.restore(base64EncodedJSON)
    fun toBase64EncodedJSON() = properties.toBase64EncodedJSON()
}
