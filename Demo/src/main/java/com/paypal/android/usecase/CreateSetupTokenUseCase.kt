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

    suspend operator fun invoke(card: Card, returnUrl: String, customerId: String?): String {
        val cardNumber = card.number.replace("\\s".toRegex(), "")
        val cardExpiry = "${card.expirationYear}-${card.expirationMonth}"

        val cardJSON = JSONObject()
            .put("number", cardNumber)
            .put("expiry", cardExpiry)
            .put("security_code", card.securityCode)
        card.cardholderName?.let { cardJSON.put("name", it) }

        cardJSON.put("verification_method", "SCA_WHEN_REQUIRED")
        val experienceContextJSON = JSONObject()

        experienceContextJSON.put("return_url", returnUrl)
        experienceContextJSON.put("cancel_url", returnUrl)
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

        val jsonOrder = JsonParser.parseString(body.toString()) as JsonObject
        val response = sdkSampleServerAPI.createSetupToken(jsonOrder)
        val responseJSON = JSONObject(response.string())

        val setupTokenId = responseJSON.getString("id")
//        val status = responseJSON.getString("status")
//        val customerId = responseJSON.getString("customer.id")
        return setupTokenId
    }
}