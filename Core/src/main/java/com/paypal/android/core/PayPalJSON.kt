package com.paypal.android.core

import org.json.JSONException
import org.json.JSONObject

class PayPalJSON(private val json: JSONObject) {

    @Throws(JSONException::class)
    constructor(input: String?) : this(JSONObject(input ?: "{}"))

    @Throws(JSONException::class)
    fun getString(keyPath: String): String {
        var node: JSONObject = json

        val keys = keyPath.split(".").toMutableList()
        while (keys.size > 1) {
            node = node.getJSONObject(keys[0])
            keys.removeFirst()
        }
        return node.getString(keys[0])
    }
}
