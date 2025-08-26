package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.restoreFromBase64EncodedJSON
import com.paypal.android.corepayments.toBase64EncodedJSON

internal class PayPalWebCheckoutSessionStore {
    var authState: String? = null

    private val properties: MutableMap<String, String?> = mutableMapOf()

    fun clear() = properties.clear()
    fun restore(base64EncodedJSON: String) =
        properties.restoreFromBase64EncodedJSON(base64EncodedJSON)

    fun toBase64EncodedJSON() = properties.toBase64EncodedJSON()
}
