package com.paypal.android.core.graphql.common

import org.json.JSONArray
import org.json.JSONObject

fun JSONArray?.toStringsList(): List<String> {
    return if (this == null) emptyList()
    else {
        MutableList(this.length()) {
            this.getString(it)
        }
    }
}

fun JSONObject.getJSONArrayOrNull(key: String): JSONArray? {
    return if (has(key)) this.getJSONArray(key)
    else null
}
