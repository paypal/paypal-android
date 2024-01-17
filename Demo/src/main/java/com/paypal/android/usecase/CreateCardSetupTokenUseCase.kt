package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResponseParser
import com.paypal.android.cardpayments.threedsecure.SCA
import org.json.JSONObject
import javax.inject.Inject

class CreateCardSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {
    private val responseParser = SDKSampleServerResponseParser()

    suspend operator fun invoke(perform3DS: Boolean): CardSetupToken {
        // create a payment token with an empty card attribute; the merchant app will
        // provide the card's details through the SDK

        val request = if (perform3DS) {
            // language=JSON
            """
            {
              "payment_source": {
                "card": {
                  "verification_method": "SCA_ALWAYS",
                  "experience_context": {
                    "return_url": "com.paypal.android.demo://vault/success",
                    "cancel_url": "com.paypal.android.demo://vault/cancel"
                  }
                }
              }
            }
            """
        } else {
            // language=JSON
            """
            {
              "payment_source": {
                "card": {}
              }
            }
            """
        }

        val jsonOrder = JsonParser.parseString(request) as JsonObject
        val response = sdkSampleServerAPI.createSetupToken(jsonOrder)
        val responseJSON = JSONObject(response.string())
        val approveHref = responseParser.findLinkHref(responseJSON, "approve")

        val customerJSON = responseJSON.getJSONObject("customer")
        return CardSetupToken(
            id = responseJSON.getString("id"),
            customerId = customerJSON.getString("id"),
            status = responseJSON.getString("status"),
            threeDSHref = approveHref
        )
    }
}
