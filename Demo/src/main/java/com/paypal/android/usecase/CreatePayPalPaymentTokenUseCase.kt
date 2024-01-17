package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreatePayPalPaymentTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {
    suspend operator fun invoke(setupToken: PayPalSetupToken): SDKSampleServerResult<PayPalPaymentToken, Exception> =
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
            sdkSampleServerAPI.createPayPalPaymentToken(requestJson)
        }
}
