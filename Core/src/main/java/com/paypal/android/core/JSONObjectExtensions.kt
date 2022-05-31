package com.paypal.android.core

import org.json.JSONObject

/**
 * Map a JSON object to a specified type using a transform function.
 */
inline fun <T> JSONObject.map(
    transform: (JSONObject) -> T
): T = transform(this)
