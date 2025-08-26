package com.paypal.android.corepayments

import android.util.Base64
import androidx.annotation.RestrictTo
import org.json.JSONObject
import java.nio.charset.StandardCharsets

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun MutableMap<String, String?>.restoreFromBase64EncodedJSON(base64EncodedJSON: String) {
    clear()

    val data = Base64.decode(base64EncodedJSON, Base64.DEFAULT)
    val requestJSONString = String(data, StandardCharsets.UTF_8)
    val requestJSON = JSONObject(requestJSONString)
    val properties = mutableMapOf<String, Any>().apply {
        requestJSON.keys().forEach { put(it, requestJSON[it]) }
    }
    properties.putAll(properties)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun MutableMap<String, String?>.toBase64EncodedJSON(): String {
    val propertiesAsJSON = JSONObject()
    forEach { (key, value) -> propertiesAsJSON.putOpt(key, value) }

    val requestJSONBytes: ByteArray? =
        propertiesAsJSON.toString().toByteArray(StandardCharsets.UTF_8)
    return Base64.encodeToString(requestJSONBytes, Base64.DEFAULT)
}
