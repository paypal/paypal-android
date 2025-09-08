package com.paypal.android.corepayments

import android.util.Base64
import androidx.annotation.RestrictTo
import org.json.JSONObject
import java.nio.charset.StandardCharsets

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SessionStore(private val properties: MutableMap<String, String?> = mutableMapOf()) {

    operator fun get(key: String): String? = properties[key]

    operator fun set(key: String, value: String?) {
        properties[key] = value
    }

    fun clear() = properties.clear()

    fun restore(base64EncodedJSON: String) {
        val data = Base64.decode(base64EncodedJSON, Base64.DEFAULT)
        val requestJSONString = String(data, StandardCharsets.UTF_8)
        val requestJSON = JSONObject(requestJSONString)

        properties.clear()
        requestJSON.keys().forEach { properties.put(it, requestJSON[it] as? String) }
    }

    fun toBase64EncodedJSON(): String {
        val propertiesAsJSON = JSONObject()
        properties.forEach { (key, value) -> propertiesAsJSON.put(key, value) }
        val requestJSONBytes: ByteArray? =
            propertiesAsJSON.toString().toByteArray(StandardCharsets.UTF_8)
        // use bitwise OR to combine flags
        val flags = Base64.DEFAULT or Base64.NO_WRAP
        return Base64.encodeToString(requestJSONBytes, flags)
    }
}
