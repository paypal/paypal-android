package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreatePayPalSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(): SDKSampleServerResult<PayPalSetupToken, Exception> =
        withContext(Dispatchers.IO) {
            // language=JSON
            val request = """
            {
              "payment_source": {
                "paypal": {
                  "usage_type": "MERCHANT",
                  "experience_context": {
                    "vault_instruction": "ON_PAYER_APPROVAL",
                    "return_url": "com.paypal.android.demo://vault/success",
                    "cancel_url": "com.paypal.android.demo://vault/cancel"
                  }
                }
              }
            }
        """

            // Ref: https://stackoverflow.com/a/19610814
            val body = request.replace("\\/", "/")

            val jsonOrder = JsonParser.parseString(body) as JsonObject
            sdkSampleServerAPI.createPayPalSetupToken(jsonOrder)
        }
}
