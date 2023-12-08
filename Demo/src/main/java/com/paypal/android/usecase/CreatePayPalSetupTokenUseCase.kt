package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.requests.PayPalPaymentSource
import com.paypal.android.api.requests.PaymentSource
import com.paypal.android.api.requests.SetupTokenRequest
import com.paypal.android.api.requests.UsageType
import com.paypal.android.api.requests.VaultInstruction
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutVaultExperienceContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class CreatePayPalSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(
        vaultExperienceContext: PayPalWebCheckoutVaultExperienceContext
    ): PayPalSetupToken {

        val request = SetupTokenRequest().apply {
            paymentSource[PaymentSource.PayPal] = PayPalPaymentSource().apply {
                usageType = UsageType.Merchant
                experienceContext.apply {
                    vaultInstruction = VaultInstruction.OnPayerApproval
                    returnUrl = vaultExperienceContext.returnUrl
                    cancelUrl = vaultExperienceContext.cancelUrl
                }
            }
        }

        // Ref: https://stackoverflow.com/a/19610814
        val body = Json.encodeToString(request).replace("\\/", "/")

        val jsonOrder = JsonParser.parseString(body) as JsonObject
        val response = sdkSampleServerAPI.createSetupToken(jsonOrder)
        val responseJSON = JSONObject(response.string())

        val customerJSON = responseJSON.getJSONObject("customer")

        val linksJSON = responseJSON.optJSONArray("links") ?: JSONArray()
        var approveVaultHref: String? = null
        for (i in 0 until linksJSON.length()) {
            val link = linksJSON.getJSONObject(i)
            if (link.getString("rel") == "approve") {
                approveVaultHref = link.getString("href")
                break
            }
        }

        return PayPalSetupToken(
            id = responseJSON.getString("id"),
            customerId = customerJSON.getString("id"),
            status = responseJSON.getString("status"),
            approveVaultHref = approveVaultHref
        )
    }
}
