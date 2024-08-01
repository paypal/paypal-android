package com.paypal.android.corepayments

import org.json.JSONObject

internal fun JSONObject.optBooleanAtKeyPath(keyPath: String, fallback: Boolean = false): Boolean {
    val keys = keyPath.split(".").toMutableList()
    if (keys.isNotEmpty()) {
        // with _this_ as the root JSON node, iteratively search the key path until we reach the last key
        var targetObject: JSONObject? = this
        while (keys.size > 1) {
            val key = keys.removeFirst()
            targetObject = targetObject?.optJSONObject(key)
        }
        return targetObject?.optBoolean(keys[0]) ?: fallback
    }
    return fallback
}
