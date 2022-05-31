package com.paypal.android.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Map an array of JSON objects to a uniform type using a transform function.
 */
inline fun <T> JSONArray.map(
    transform: (JSONObject) -> T
): List<T> =
    (0 until length()).map { index ->
        transform(getJSONObject(index))
    }