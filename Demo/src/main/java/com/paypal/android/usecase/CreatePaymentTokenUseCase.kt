package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.services.SDKSampleServerAPI
import org.json.JSONObject
import javax.inject.Inject

class CreatePaymentTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(setupTokenId: String): String {
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

        return responseJSON.getString("id")
//        val customerJSON = responseJSON.getJSONObject("customer")
//        val cardJSON = responseJSON.getJSONObject("payment_source").getJSONObject("card")
    }
}
