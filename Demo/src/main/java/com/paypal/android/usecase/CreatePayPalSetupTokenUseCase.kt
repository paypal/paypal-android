package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutVaultExperienceContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class CreatePayPalSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(
        vaultExperienceContext: PayPalWebCheckoutVaultExperienceContext
    ): PayPalSetupToken {

        // language=JSON
        val request = """
            {
              "payment_source": {
                "paypal": {
                  "usage_type": "MERCHANT",
                  "experience_context": {
                    "vault_instruction": "ON_PAYER_APPROVAL",
                    "return_url": "${vaultExperienceContext.returnUrl}",
                    "cancel_url": "${vaultExperienceContext.cancelUrl}"
                  }
                }
              }
            }
        """

        // Ref: https://stackoverflow.com/a/19610814
        val body = request.replace("\\/", "/")

        val jsonOrder = JsonParser.parseString(body) as JsonObject
        val response = sdkSampleServerAPI.createSetupToken(jsonOrder)

        val responseJSON = JSONObject(response.string())
        val customerJSON = responseJSON.getJSONObject("customer")
        val approveVaultHref = findApprovalHref(responseJSON)

        return PayPalSetupToken(
            id = responseJSON.getString("id"),
            customerId = customerJSON.getString("id"),
            status = responseJSON.getString("status"),
            approveVaultHref = approveVaultHref
        )
    }

    private fun findApprovalHref(responseJSON: JSONObject): String? {
        val linksJSON = responseJSON.optJSONArray("links") ?: JSONArray()
        for (i in 0 until linksJSON.length()) {
            val link = linksJSON.getJSONObject(i)
            if (link.getString("rel") == "approve") {
                return link.getString("href")
            }
        }
        return null
    }
}
