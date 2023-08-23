package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.SetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import org.json.JSONObject
import javax.inject.Inject

class CreateSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(customerId: String?): SetupToken {
        // create a payment token with an empty card attribute; the merchant app will provide
        // the card's details through the SDK
        val cardJSON = JSONObject()
        cardJSON.put("verification_method", "SCA_WHEN_REQUIRED")

        val experienceContextJSON = JSONObject()
        experienceContextJSON.put("return_url", "https://example.com/returnUrl")
        experienceContextJSON.put("cancel_url", "https://example.com/cancelUrl")
        cardJSON.put("experience_context", experienceContextJSON)


        val paymentSourceJSON = JSONObject()
        paymentSourceJSON.put("card", cardJSON)

        val requestJSON = JSONObject()
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
        return SetupToken(
            id = responseJSON.getString("id"),
            customerId = customerJSON.getString("id"),
            status = responseJSON.getString("status")
        )
    }
}
