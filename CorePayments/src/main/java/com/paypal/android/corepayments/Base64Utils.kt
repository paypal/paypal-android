package com.paypal.android.corepayments

import android.util.Base64
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object Base64Utils {

    fun parseBase64EncodedJSON(input: String): JSONObject? {
        val data = Base64.decode(input, Base64.DEFAULT)
        val requestJSONString = String(data, StandardCharsets.UTF_8)
        return try {
            JSONObject(requestJSONString)
        } catch (ex: JSONException) {
            null
        }
    }
}