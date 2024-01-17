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
            try {
                val setupToken = createCardSetupToken()
                UseCaseResult.Success(setupToken)
            } catch (e: Exception) {
                UseCaseResult.Failure(e)
            }
        }

    private suspend fun createCardSetupToken(): CardSetupToken {
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
        val response = sdkSampleServerAPI.createSetupToken(jsonOrder)
        val responseJSON = JSONObject(response.string())

        val customerJSON = responseJSON.getJSONObject("customer")
        return CardSetupToken(
            id = responseJSON.getString("id"),
            customerId = customerJSON.getString("id"),
            status = responseJSON.getString("status"),
        )
    }
}
