package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.CardPaymentToken
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateCardPaymentTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(setupToken: CardSetupToken): SDKSampleServerResult<CardPaymentToken, Exception> =
        withContext(Dispatchers.IO) {
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
            sdkSampleServerAPI.createPaymentToken(requestJson)
        }
}
