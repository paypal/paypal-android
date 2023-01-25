package com.paypal.android.corepayments.graphql.common

import org.json.JSONArray
import org.json.JSONObject

fun JSONArray?.toStringsList(): List<String> =
    if (this == null) emptyList()
    else MutableList(this.length()) {
        this.getString(it)
    }

fun JSONObject.getJSONArrayOrNull(key: String): JSONArray? {
    return if (has(key)) this.getJSONArray(key)
    else null
}
