package com.paypal.android.core

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class PaymentsJSON(val json: JSONObject) {

    @Throws(JSONException::class)
    constructor(input: String) : this(JSONObject(input))

    @Throws(JSONException::class)
    fun getString(keyPath: String): String {
        val nodeResult = searchNode(keyPath)
        val keys = nodeResult.keys
        return nodeResult.node.getString(keys[0])
    }

    fun getJSONArray(keyPath: String): JSONArray {
        val nodeResult = searchNode(keyPath)
        val keys = nodeResult.keys
        return nodeResult.node.getJSONArray(keys[0])
    }

    fun getJSONObject(keyPath: String): JSONObject {
        val nodeResult = searchNode(keyPath)
        val keys = nodeResult.keys
        return nodeResult.node.getJSONObject(keys[0])
    }

    // TODO: consolidate json keypath logic
    fun optGetJSONObject(keyPath: String): JSONObject? {
        val keys = keyPath.split(".").toMutableList()

        var node: JSONObject? = json
        while (keys.size > 0) {
            node = node?.optJSONObject(keys[0])
            keys.removeFirst()
        }
        return node
    }

    private fun searchNode(keyPath: String): NodeResult {
        var node: JSONObject = json

        val keys = keyPath.split(".").toMutableList()
        while (keys.size > 1) {
            node = node.getJSONObject(keys[0])
            keys.removeFirst()
        }
        return NodeResult(node, keys)
    }

    fun getLinkHref(rel: String): String? {
        val linksArray = json.optJSONArray("links") ?: JSONArray()
        val links = (0 until linksArray.length()).map { linksArray.getJSONObject(it) }

        val targetLink = links.firstOrNull { rel == it.getString("rel") }
        return targetLink?.optString("href")
    }

    // TODO: consolidate json keypath logic
    fun optGetJSONArray(keyPath: String): JSONArray? {
        val keys = keyPath.split(".").toMutableList()

        var node: JSONObject? = json
        while (keys.size > 1) {
            node = node?.optJSONObject(keys[0])
            keys.removeFirst()
        }

        return node?.optJSONArray(keys[0])
    }

    fun <T> optMapJSONArray(keyPath: String, transform: (JSONObject) -> T): List<T>? =
        optGetJSONArray(keyPath)?.map(transform)

    fun <T> optMapJSONObject(keyPath: String, transform: (JSONObject) -> T): T? =
        optGetJSONObject(keyPath)?.map(transform)

    private class NodeResult(val node: JSONObject, val keys: List<String>)
}

fun JSONObject.optNullableString(name: String, fallback: String? = null): String? {
    return if (this.has(name) && !this.isNull(name)) {
        this.getString(name)
    } else {
        fallback
    }
}

fun JSONObject.containsKey(key: String) = this.has(key) && !this.isNull(key)

fun JSONObject.optNullableJSONObject(name: String): JSONObject? {
    return if (this.containsKey(name)) {
        this.getJSONObject(name)
    } else {
        null
    }
}
