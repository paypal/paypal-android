package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.Card
import org.json.JSONObject
import javax.inject.Inject

class CreateSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(customerId: String?): String {
        // create a payment token with an empty card attribute; the merchant app will provide
        // the card's details through the SDK
        val cardJSON = JSONObject()
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

        val jsonOrder = JsonParser.parseString(body.toString()) as JsonObject
        val response = sdkSampleServerAPI.createSetupToken(jsonOrder)
        val responseJSON = JSONObject(response.string())

        val setupTokenId = responseJSON.getString("id")
//        val status = responseJSON.getString("status")
//        val customerId = responseJSON.getString("customer.id")
        return setupTokenId
    }
}