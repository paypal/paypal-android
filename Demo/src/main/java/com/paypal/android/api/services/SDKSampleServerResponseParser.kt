package com.paypal.android.api.services

import org.json.JSONArray
import org.json.JSONObject

class SDKSampleServerResponseParser() {

    fun findLinkHref(responseJSON: JSONObject, rel: String): String? {
        val linksJSON = responseJSON.optJSONArray("links") ?: JSONArray()
        for (i in 0 until linksJSON.length()) {
            val link = linksJSON.getJSONObject(i)
            if (link.getString("rel") == rel) {
                return link.getString("href")
            }
        }
        return null
    }
}
