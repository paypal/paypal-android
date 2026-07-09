package com.paypal.android.venmo

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.api.GetFundingEligibility
import com.paypal.android.corepayments.model.APIResult
import java.util.UUID

class VenmoClient(
    private val coreConfig: CoreConfig,
    private val ccoAPI: UpdateClientConfigAPI,
    private val getFundingEligibility: GetFundingEligibility,
) {

    constructor(context: Context, config: CoreConfig) : this(
        coreConfig = config,
        UpdateClientConfigAPI(context, config),
        GetFundingEligibility(config)
    )

    suspend fun start(
        activity: ComponentActivity,
        orderId: String,
        returnUrl: String,
        currency: String = "USD"
    ) {
        // Check funding eligibility for Venmo
        val eligibilityResult = getFundingEligibility(
            context = activity,
            clientId = coreConfig.clientId,
            currency = currency
        )

        when (eligibilityResult) {
            is APIResult.Success -> {
                Log.d("VenmoClient", "GetFundingEligibility success: ${eligibilityResult.data}")
                if (!eligibilityResult.data.venmoEligible) {
                    throw Exception("Venmo is not eligible for this transaction")
                }
            }

            is APIResult.Failure -> {
                Log.d("VenmoClient", "GetFundingEligibility failure: ${eligibilityResult.error}")
                throw Exception(eligibilityResult.error.errorDescription)
            }
        }

        // Venmo is eligible, proceed with CCO update
//        val ccoUpdateResult =
//            ccoAPI.updateClientConfig(tokenId = orderId, fundingSource = "venmo")
//        when (ccoUpdateResult) {
//            UpdateClientConfigResult.Success -> {
//                Log.d("venmo", "CCO Update Success")
//            }
//
//            is UpdateClientConfigResult.Failure -> {
//                Log.d("venmo", "CCO Update Failure")
//            }
//        }

        // FROM: VenmoAppSwitch
        val localVenmoBaseUrl = "https://account.venmo.com/go/web/paypal"
//        val localVenmoBaseUrl = "https://account.qa.venmo.com/go/web/paypal"
//        val localVenmoBaseUrl = "https://venmo.com/smart/checkout/venmo"
//        val localVenmoBaseUrl = "https://www.paypal.com/smart/checkout/venmo"
//        val sandboxVenmoBaseUrl = "https://www.sandbox.paypal.com/smart/checkout/venmo"
        val appSwitchUri = localVenmoBaseUrl.toUri()
            .buildUpon()
            .appendQueryParameter("buttonSessionID", UUID.randomUUID().toString())
            .appendQueryParameter("buyerCountry", "US")
//            .appendQueryParameter("channel", "in-app")
            .appendQueryParameter("channel", "in-app")
//            .appendQueryParameter("channel", "mobile-web")
            .appendQueryParameter("commit", "true")
            .appendQueryParameter("domain", "sdk.paypal.com")
            .appendQueryParameter("enableFunding", "venmo")
            .appendQueryParameter("env", "qa")
            .appendQueryParameter("facilitatorAccessToken", "")
            .appendQueryParameter("fundingSource", "venmo")
            .appendQueryParameter("return_flow", "auto")
//            .appendQueryParameter("orderID", orderId)
            .appendQueryParameter("token", orderId)
            .appendQueryParameter("pageUrl", returnUrl)
            .appendQueryParameter("sessionUID", UUID.randomUUID().toString())
            .appendQueryParameter("sdkMeta", "")
//            .appendQueryParameter("clientID", coreConfig.clientId)
//            .appendQueryParameter("merchantId", "V9YP27HFNG2LW")
            .build()
        activity.startActivity(Intent(Intent.ACTION_VIEW, appSwitchUri))

        // FROM: VenmoWebProductFlow.ts (Sandbox)

        // FROM: VenmoAppSwitchProductFlow.ts (Sandbox)
    }

    fun finishStart(intent: Intent): VenmoFinishStartResult? =
        intent.data?.let { uri ->
            // Ref: https://ppcp-mobile-demo-sandbox-87bbd7f0a27f.herokuapp.com/success?token=3AA109843N208505T&PayerID=DZJ6MD58L5YY6&approved=true
            val token = uri.getQueryParameter("token")
            val payerId = uri.getQueryParameter("PayerID")
            val approved = uri.getQueryParameter("approved")
            if (token != null && payerId != null && approved != null) {
                VenmoFinishStartResult.Success(token, payerId, approved.toBoolean())
            } else {
                val error = PayPalSDKError(123, "Unable to parse deep link.")
                VenmoFinishStartResult.Failure(error)
            }
        }
}
