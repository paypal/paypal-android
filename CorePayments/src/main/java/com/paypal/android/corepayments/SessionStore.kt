package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class SessionStore {
    val properties: MutableMap<String, String?> = mutableMapOf()

    fun clear() = properties.clear()
    fun restore(base64EncodedJSON: String) =
        properties.restoreFromBase64EncodedJSON(base64EncodedJSON)

    fun toBase64EncodedJSON() = properties.toBase64EncodedJSON()
}
