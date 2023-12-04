package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.SetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class CreateCardSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(customerId: String?): SetupToken {
        val requestJSON = JSONObject()
        // create a payment token with an empty card attribute; the merchant app will
        // provide the card's details through the SDK
        val cardJSON = JSONObject()
        val paymentSourceJSON = JSONObject()
        paymentSourceJSON.put("card", cardJSON)

        requestJSON.put("payment_source", paymentSourceJSON)

        if (!customerId.isNullOrEmpty()) {
            val customerJSON = JSONObject()
                .put("id", customerId)
            requestJSON.put("customer", customerJSON)
        }

        // Ref: https://stackoverflow.com/a/19610814
        val body = requestJSON.toString().replace("\\/", "/")

        val jsonOrder = JsonParser.parseString(body) as JsonObject
        val response = sdkSampleServerAPI.createSetupToken(jsonOrder)
        val responseJSON = JSONObject(response.string())

        val customerJSON = responseJSON.getJSONObject("customer")

        val linksJSON = responseJSON.optJSONArray("links") ?: JSONArray()
        var approveVaultHref: String? = null
        for (i in 0 until linksJSON.length()) {
            val link = linksJSON.getJSONObject(i)
            if (link.getString("rel") == "approve") {
                approveVaultHref = link.getString("href")
                break
            }
        }

        return SetupToken(
            id = responseJSON.getString("id"),
            customerId = customerJSON.getString("id"),
            status = responseJSON.getString("status"),
            approveVaultHref = approveVaultHref
        )
    }
}
