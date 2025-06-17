package com.paypal.android.usecase

import com.paypal.android.api.model.Order
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.models.OrderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class CreateOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(request: OrderRequest): SDKSampleServerResult<Order, Exception> =
        withContext(Dispatchers.IO) {
            val amountJSON = JSONObject()
                .put("currency_code", "USD")
                .put("value", "10.99")

            val purchaseUnitJSON = JSONObject()
                .put("amount", amountJSON)

            val orderRequest = JSONObject()
                .put("intent", request.intent)
                .put("purchase_units", JSONArray().put(purchaseUnitJSON))

            if (request.shouldVault) {
                val vaultJSON = JSONObject()
                    .put("store_in_vault", "ON_SUCCESS")

                val cardAttributesJSON = JSONObject()
                    .put("vault", vaultJSON)

                val cardJSON = JSONObject()
                    .put("attributes", cardAttributesJSON)

                val paymentSourceJSON = JSONObject()
                    .put("card", cardJSON)

                orderRequest.put("payment_source", paymentSourceJSON)
            }
            if (request.enableAppSwitch) {

                val nativeAppJSON = JSONObject()
                    .put("os_type", "ANDROID")
                    .put("os_version", "35")

                val appSwitchPreferenceJSON = JSONObject()
                    .put("app_url", APP_URL)
                    .put("launch_paypal_app", true)
                    .put("os_type", "ANDROID")
                    .put("os_version", "35")
                    .put("native_app", nativeAppJSON)

                val experienceContextJSON = JSONObject()
                    .put("brand_name", "AA Logos")
                    .put("landing_page", "LOGIN")
                    .put("shipping_preference", "NO_SHIPPING")
                    .put("return_url", SUCCESS_URL)
                    .put("cancel_url", CANCEL_URL)
                    .put("payment_method_preference", "IMMEDIATE_PAYMENT_REQUIRED")
                    .put("payment_method_selected", "PAYPAL")
                    .put("user_action", "PAY_NOW")
                    .put("app_switch_preference", appSwitchPreferenceJSON)

                val paypalJSON = JSONObject()
                    .put("email_address", "sb-ze5t741841447@personal.example.com")
                    .put("experience_context", experienceContextJSON)

                val paymentSourceJSON = JSONObject()
                    .put("paypal", paypalJSON)

                orderRequest.put("payment_source", paymentSourceJSON)
            }
            sdkSampleServerAPI.createOrder(orderRequest)
        }
}

private const val APP_URL = "com.paypal.android.demo://"
private const val SUCCESS_URL = "${APP_URL}success"
private const val CANCEL_URL = "${APP_URL}cancel"
