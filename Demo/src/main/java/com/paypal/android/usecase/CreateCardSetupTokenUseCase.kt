package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

class CreateCardSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(): UseCaseResult<CardSetupToken, Exception> =
        withContext(Dispatchers.IO) {
            // create a payment token with an empty card attribute; the merchant app will
            // provide the card's details through the SDK

            // language=JSON
            val request = """
            {
              "payment_source": {
                "card": {}
              }
            }
            """

            val jsonOrder = JsonParser.parseString(request) as JsonObject
            sdkSampleServerAPI.createSetupToken(jsonOrder)
        }
}
