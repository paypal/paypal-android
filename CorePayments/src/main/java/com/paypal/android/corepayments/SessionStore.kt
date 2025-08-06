package com.paypal.android.corepayments

import android.util.Base64
import androidx.annotation.RestrictTo
import org.json.JSONObject
import java.nio.charset.StandardCharsets

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SessionStore(private val properties: MutableMap<String, String?> = mutableMapOf()) {

    fun get(key: String): String? {
        return properties[key]
    }

    fun put(key: String, value: String?) {
        properties[key] = value
    }

    fun restore(base64EncodedJSON: String) {
        clear()

        val data = Base64.decode(base64EncodedJSON, Base64.DEFAULT)
        val requestJSONString = String(data, StandardCharsets.UTF_8)
        val requestJSON = JSONObject(requestJSONString)
        val properties = mutableMapOf<String, Any>().apply {
            requestJSON.keys().forEach { put(it, requestJSON[it]) }
        }
        properties.putAll(properties)
    }

    fun clear() {
        properties.clear()
    }

    fun toBase64EncodedJSON(): String {
        val propertiesAsJSON = JSONObject(properties)
        val requestJSONBytes: ByteArray? =
            propertiesAsJSON.toString().toByteArray(StandardCharsets.UTF_8)
        return Base64.encodeToString(requestJSONBytes, Base64.DEFAULT)
    }
}