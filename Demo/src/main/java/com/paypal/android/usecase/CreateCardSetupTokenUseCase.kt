package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.cardpayments.threedsecure.SCA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateCardSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(sca: SCA): SDKSampleServerResult<CardSetupToken, Exception> =
        withContext(Dispatchers.IO) {
            // create a payment token with an empty card attribute; the merchant app will
            // provide the card's details through the SDK
            // language=JSON
            val request = """
            {
                "payment_source": {
                    "card": {
                        "verification_method": "${sca.name}",
                        "experience_context": {
                            "return_url": "com.paypal.android.demo://vault/success",
                            "cancel_url": "com.paypal.android.demo://vault/cancel"
                        }
                    }
                }
            }
            """
            val jsonOrder = JsonParser.parseString(request) as JsonObject
            sdkSampleServerAPI.createSetupToken(jsonOrder)
        }
}
