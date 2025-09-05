package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SessionStore(private val properties: MutableMap<String, String?> = mutableMapOf()) {

    operator fun get(key: String): String? = properties[key]

    operator fun set(key: String, value: String?) {
        properties[key] = value
    }

    fun clear() = properties.clear()
    fun restore(base64EncodedJSON: String) =
        properties.restoreFromBase64EncodedJSON(base64EncodedJSON)

    fun toBase64EncodedJSON() = properties.toBase64EncodedJSON()
}