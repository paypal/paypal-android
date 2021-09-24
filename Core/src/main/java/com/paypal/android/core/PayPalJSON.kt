package com.paypal.android.core

import org.json.JSONObject

class PayPalJSON(private val json: JSONObject) {

    constructor(input: String?) : this(JSONObject(input ?: "{}"))

    fun optString(keyPath: String): String? {
        var node: JSONObject = json

        val keys = keyPath.split(".").toMutableList()
        while (keys.size > 1) {
            node = node.getJSONObject(keys[0])
            keys.removeFirst()
        }
        return node.optString(keys[0])
    }
}
