package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.SetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.models.PaymentMethod
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class CreateSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(paymentMethod: PaymentMethod, customerId: String?): SetupToken {
        val requestJSON = JSONObject()
        when (paymentMethod) {
            PaymentMethod.CARD -> {
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
            }

            PaymentMethod.PAYPAL -> {
                val payPalJSON = JSONObject()
                payPalJSON.put("usage_type", "MERCHANT")

                val experienceContextJSON = JSONObject()
                experienceContextJSON.put("vault_instruction", "ON_PAYER_APPROVAL")
                experienceContextJSON.put("return_url", "com.paypal.android.demo://example.com/return_url")
                experienceContextJSON.put("cancel_url", "com.paypal.android.demo://example.com/cancel_url")
                payPalJSON.put("experience_context", experienceContextJSON)

                val paymentSourceJSON = JSONObject()
                paymentSourceJSON.put("paypal", payPalJSON)
                requestJSON.put("payment_source", paymentSourceJSON)
            }
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
