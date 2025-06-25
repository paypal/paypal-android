package com.paypal.android.usecase

import com.google.gson.JsonParser
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

private const val CANCEL_URL = "com.paypal.android.demo://vault/cancel"

private const val SUCCESS_URL = "com.paypal.android.demo://vault/success"

class CreatePayPalSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(appSwitchEnabled: Boolean): SDKSampleServerResult<PayPalSetupToken, Exception> =
        withContext(Dispatchers.IO) {
            val request = JSONObject().apply {
                if (appSwitchEnabled) {

                    val nativeAppJSON = JSONObject()
                        .put("os_type", "ANDROID")
                        .put("os_version", "35")

                    val appSwitchPreferenceJSON = JSONObject()
//                        .put("app_url", app_url)
//                        .put("launch_paypal_app", true)
                        .put("native_app", nativeAppJSON)

                    val experienceContextJSON = JSONObject()
//                        .put("brand_name", "AA Logos")
//                        .put("shipping_preference", "NO_SHIPPING")
//                        .put("vault_instruction", "ON_PAYER_APPROVAL")
                        .put("return_url", SUCCESS_URL)
                        .put("cancel_url", CANCEL_URL)
//                        .put("payment_method_preference", "IMMEDIATE_PAYMENT_REQUIRED")
//                        .put("payment_method_selected", "PAYPAL")
//                        .put("user_action", "CONTINUE")
                        .put("app_switch_context", appSwitchPreferenceJSON)

                    val paypalJSON = JSONObject()
                        .put("usage_type", "MERCHANT")
                        .put("email_address", "sb-ze5t741841447@personal.example.com")
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
