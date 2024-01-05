package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import org.json.JSONObject
import javax.inject.Inject

class CreatePayPalPaymentTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(setupToken: PayPalSetupToken): PayPalPaymentToken {
        // language=JSON
        val request = """
            {
              "payment_source": {
                "token": {
                  "id": "${setupToken.id}",
                  "type": "SETUP_TOKEN"
                }
              }
            }
        """

        val requestJson = JsonParser.parseString(request) as JsonObject
        val response = sdkSampleServerAPI.createPaymentToken(requestJson)
        val responseJSON = JSONObject(response.string())
        val customerJSON = responseJSON.getJSONObject("customer")

        return PayPalPaymentToken(
            id = responseJSON.getString("id"),
            customerId = customerJSON.getString("id")
        )
    }
}
