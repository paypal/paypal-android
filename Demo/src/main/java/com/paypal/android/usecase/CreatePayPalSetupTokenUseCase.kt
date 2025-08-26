package com.paypal.android.usecase

import com.google.gson.JsonParser
import com.paypal.android.DemoConstants.APP_URL
import com.paypal.android.DemoConstants.CANCEL_URL
import com.paypal.android.DemoConstants.SUCCESS_URL
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

class CreatePayPalSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(appSwitchEnabled: Boolean): SDKSampleServerResult<PayPalSetupToken, Exception> =
        withContext(Dispatchers.IO) {
            val request = JSONObject().apply {
                if (appSwitchEnabled) {

                    val nativeAppJSON = JSONObject()
                        .put("app_url", APP_URL)
                    val experienceContextJSON = JSONObject()
                        .put("return_url", SUCCESS_URL)
                        .put("cancel_url", CANCEL_URL)
                        .put("native_app", nativeAppJSON)

                    val paypalJSON = JSONObject()
                        .put("usage_type", "MERCHANT")
                        .put("experience_context", experienceContextJSON)

                    val paymentSourceJSON = JSONObject()
                        .put("paypal", paypalJSON)

                    put("payment_source", paymentSourceJSON)
                } else {
                    val paymentSourceJSON = JSONObject().apply {
                        val paypalJSON = JSONObject().apply {
                            put("usage_type", "MERCHANT")
                            put("experience_context", JSONObject().apply {
                                put("vault_instruction", "ON_PAYER_APPROVAL")
                                put("return_url", SUCCESS_URL)
                                put("cancel_url", CANCEL_URL)
                            })
                        }
                        put("paypal", paypalJSON)
                    }
                    put("payment_source", paymentSourceJSON)
                }
            }
            val jsonObject = JsonParser.parseString(request.toString()).asJsonObject
            sdkSampleServerAPI.createPayPalSetupToken(jsonObject)
        }
}
