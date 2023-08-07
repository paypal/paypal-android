package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.PaymentToken
import com.paypal.android.api.services.SDKSampleServerAPI
import org.json.JSONObject
import javax.inject.Inject

class CreatePaymentTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(setupTokenId: String): PaymentToken {
        // language=JSON
        val request = """
            {
              "payment_source": {
                "token": {
                  "id": ${setupTokenId},
                  "type": "SETUP_TOKEN"
                }
              }
            }
        """.trimIndent()
        val requestJson = JsonParser.parseString(request) as JsonObject
        val response = sdkSampleServerAPI.createPaymentToken(requestJson)
        val responseJSON = JSONObject(response.string())

        val customerJSON = responseJSON.getJSONObject("customer")
        val cardJSON = responseJSON.getJSONObject("payment_source").getJSONObject("card")

        return PaymentToken(
            id = responseJSON.getString("id"),
            customerId = customerJSON.getString("id"),
            cardLast4 = cardJSON.getString("last_digits"),
            cardBrand = cardJSON.getString("brand")
        )
    }
}
