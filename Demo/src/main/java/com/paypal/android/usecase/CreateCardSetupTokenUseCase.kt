package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateCardSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    // TODO: pass SCA enum as parameter instead of perform3DS
    suspend operator fun invoke(perform3DS: Boolean): SDKSampleServerResult<CardSetupToken, Exception> =
        withContext(Dispatchers.IO) {
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
            sdkSampleServerAPI.createSetupToken(jsonOrder)
        }
}
