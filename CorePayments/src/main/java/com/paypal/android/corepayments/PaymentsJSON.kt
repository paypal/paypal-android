package com.paypal.android.corepayments

import androidx.annotation.RestrictTo
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentsJSON(val json: JSONObject) {

    @Throws(JSONException::class)
    constructor(input: String) : this(JSONObject(input))

    @Throws(JSONException::class)
    fun getString(keyPath: String): String {
        val nodeResult = searchNode(keyPath)
        val keys = nodeResult.keys
        return nodeResult.node.getString(keys[0])
    }

    fun optString(keyPath: String): String? {
        val keys = keyPath.split(".").toMutableList()

        var node: JSONObject? = json
        while (keys.size > 1) {
            node = node?.optJSONObject(keys[0])
            keys.removeAt(0)
        }

        return node?.optString(keys[0])
    }

    fun optGetObject(keyPath: String): PaymentsJSON? {
        val keys = keyPath.split(".").toMutableList()

        var node: JSONObject? = json
        while (keys.size > 0) {
            node = node?.optJSONObject(keys[0])
            keys.removeAt(0)
        }
        return node?.let { PaymentsJSON(it) }
    }

    private fun searchNode(keyPath: String): NodeResult {
        var node: JSONObject = json

        val keys = keyPath.split(".").toMutableList()
        while (keys.size > 1) {
            node = node.getJSONObject(keys[0])
            keys.removeAt(0)
        }
        return NodeResult(node, keys)
    }

    fun getLinkHref(rel: String): String? {
        val linksArray = json.optJSONArray("links") ?: JSONArray()
        val links = (0 until linksArray.length()).map { linksArray.getJSONObject(it) }

        val targetLink = links.firstOrNull { rel == it.getString("rel") }
        return targetLink?.optString("href")
    }

    private fun optGetJSONArray(keyPath: String): JSONArray? {
        val keys = keyPath.split(".").toMutableList()

        var node: JSONObject? = json
        while (keys.size > 1) {
            node = node?.optJSONObject(keys[0])
            keys.removeAt(0)
        }

        return node?.optJSONArray(keys[0])
    }

    fun <T> optMapObject(keyPath: String, transform: (PaymentsJSON) -> T): T? =
        optGetObject(keyPath)?.let { transform(it) }

    fun <T> optMapObjectArray(keyPath: String, transform: (PaymentsJSON) -> T): List<T>? {
        return optGetJSONArray(keyPath)?.let { jsonArray ->
            (0 until jsonArray.length()).map { index ->
                val json = jsonArray.getJSONObject(index)
                transform(PaymentsJSON(json))
            }
        }
    }

    private class NodeResult(val node: JSONObject, val keys: List<String>)
}
