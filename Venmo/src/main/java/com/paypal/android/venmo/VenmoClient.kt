package com.paypal.android.venmo

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.PayPalSDKErrorCode
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.UpdateClientConfigResult
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

    suspend fun isEligible(
        context: Context
    ): VenmoEligibilityResult {
        require(coreConfig.clientId.isNotBlank()) { "Client ID cannot be blank" }

        return try {
            val eligibilityResult = getFundingEligibility(
                context = context,
                clientId = coreConfig.clientId,
                fundingSource = "VENMO"
            )

            when (eligibilityResult) {
                is APIResult.Success -> {
                    if (eligibilityResult.data.venmoEligible) {
                        VenmoEligibilityResult.Eligible
                    } else {
                        VenmoEligibilityResult.Ineligible("Venmo is not eligible for this transaction")
                    }
                }

                is APIResult.Failure -> {
                    VenmoEligibilityResult.Error(eligibilityResult.error)
                }
            }
        } catch (e: Exception) {
            VenmoEligibilityResult.Error(
                PayPalSDKError(
                    code = PayPalSDKErrorCode.UNKNOWN.ordinal,
                    errorDescription = e.message ?: "Unknown error checking Venmo eligibility",
                    reason = e
                )
            )
        }
    }

    suspend fun start(
        activity: ComponentActivity,
        orderId: String,
        returnUrl: String
    ) {
        require(orderId.isNotBlank()) { "Order ID cannot be blank" }
        require(returnUrl.isNotBlank()) { "Return URL cannot be blank" }

        val ccoUpdateResult = ccoAPI.updateClientConfig(
            tokenId = orderId,
            fundingSource = "venmo"
        )

        if (ccoUpdateResult is UpdateClientConfigResult.Failure) {
            throw ccoUpdateResult.error
        }

        val sessionId = UUID.randomUUID().toString()
        val venmoBaseUrl = "https://account.venmo.com/go/web/paypal"
        val appSwitchUri = venmoBaseUrl.toUri()
            .buildUpon()
            .appendQueryParameter("buttonSessionID", sessionId)
            .appendQueryParameter("buyerCountry", "US")
            .appendQueryParameter("channel", "in-app")
            .appendQueryParameter("commit", "true")
            .appendQueryParameter("domain", "sdk.paypal.com")
            .appendQueryParameter("enableFunding", "venmo")
            .appendQueryParameter("env", coreConfig.environment.venmoEnvironment)
            .appendQueryParameter("fundingSource", "venmo")
            .appendQueryParameter("return_flow", "auto")
            .appendQueryParameter("token", orderId)
            .appendQueryParameter("pageUrl", returnUrl)
            .appendQueryParameter("sessionUID", sessionId)
            .build()

        activity.startActivity(Intent(Intent.ACTION_VIEW, appSwitchUri))
    }

    fun finishStart(intent: Intent): VenmoFinishStartResult {
        val uri = intent.data
            ?: return VenmoFinishStartResult.NoResult

        // Check if this is a Venmo-related deep link by looking for Venmo-specific parameters
        val token = uri.getQueryParameter("token")
        val payerId = uri.getQueryParameter("PayerID")
        val approved = uri.getQueryParameter("approved")
        val canceled = uri.getQueryParameter("canceled")

        // If none of the expected parameters are present, this is an unrelated intent
        if (token == null && payerId == null && approved == null && canceled == null) {
            return VenmoFinishStartResult.NoResult
        }

        // Check if this is a cancellation (user pressed cancel or canceled=true)
        if (canceled == "true" || approved == "false") {
            return VenmoFinishStartResult.Failure(
                PayPalSDKError(
                    code = PayPalSDKErrorCode.CHECKOUT_ERROR.ordinal,
                    errorDescription = "User cancelled Venmo payment"
                )
            )
        }

        // Validate success case parameters
        if (token == null || payerId == null || approved == null) {
            return VenmoFinishStartResult.Failure(
                PayPalSDKError(
                    code = PayPalSDKErrorCode.DATA_PARSING_ERROR.ordinal,
                    errorDescription = "Missing required parameters in deep link: token=$token, payerId=$payerId, approved=$approved"
                )
            )
        }

        if (approved != "true") {
            return VenmoFinishStartResult.Failure(
                PayPalSDKError(
                    code = PayPalSDKErrorCode.DATA_PARSING_ERROR.ordinal,
                    errorDescription = "Invalid approval value: $approved (expected 'true' or 'false')"
                )
            )
        }

        return VenmoFinishStartResult.Success(token, payerId, approved.toBoolean())
    }
}
