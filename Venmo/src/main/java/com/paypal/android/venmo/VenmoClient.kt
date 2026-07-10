package com.paypal.android.venmo

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.PayPalSDKErrorCode
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.UpdateClientConfigResult
import com.paypal.android.corepayments.api.GetFundingEligibility
import com.paypal.android.corepayments.browserswitch.ChromeCustomTabOptions
import com.paypal.android.corepayments.browserswitch.ChromeCustomTabsClient
import com.paypal.android.corepayments.browserswitch.LaunchChromeCustomTabResult
import com.paypal.android.corepayments.model.APIResult

class VenmoClient internal constructor(
    private val context: Context,
    private val coreConfig: CoreConfig,
    private val ccoAPI: UpdateClientConfigAPI,
    private val getFundingEligibility: GetFundingEligibility,
    private val chromeCustomTabsClient: ChromeCustomTabsClient,
) {

    companion object {
        private const val VENMO = "VENMO"
        private const val CHANNEL_PARAM = "channel"
        private const val CHANNEL_VALUE = "in-app"
        private const val TOKEN_PARAM = "token"
        private const val PAGE_URL_PARAM = "pageUrl"
        private const val ENV_PARAM = "env"

        private const val INELIGIBLE_MESSAGE = "Venmo is not eligible for this transaction"
        private const val UNKNOWN_ERROR_MESSAGE = "Unknown error checking Venmo eligibility"
        private const val CANCELLATION_MESSAGE = "User cancelled Venmo payment"
        private const val MISSING_PARAMS_MESSAGE = "Missing required parameters in deep link"
        private const val INVALID_APPROVAL_MESSAGE = "Invalid approval value"

        private const val PAYER_ID_PARAM = "PayerID"
        private const val APPROVED_PARAM = "approved"
        private const val CANCELED_PARAM = "canceled"

        private const val METADATA_KEY_ORDER_ID = "order_id"
    }

    constructor(context: Context, config: CoreConfig) : this(
        context = context,
        coreConfig = config,
        ccoAPI = UpdateClientConfigAPI(context, config),
        getFundingEligibility = GetFundingEligibility(config),
        chromeCustomTabsClient = ChromeCustomTabsClient()
    )

    suspend fun isEligible(buyerCountry: String): VenmoEligibilityResult {
        require(coreConfig.clientId.isNotBlank()) { "Client ID cannot be blank" }
        require(buyerCountry.isNotBlank()) { "Buyer country cannot be blank" }

        return try {
            val eligibilityResult = getFundingEligibility(
                context = this.context,
                clientId = coreConfig.clientId,
                fundingSource = VENMO,
                buyerCountry = buyerCountry
            )

            when (eligibilityResult) {
                is APIResult.Success -> {
                    if (eligibilityResult.data.venmoEligible) {
                        VenmoEligibilityResult.Eligible
                    } else {
                        VenmoEligibilityResult.Ineligible(INELIGIBLE_MESSAGE)
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
                    errorDescription = e.message ?: UNKNOWN_ERROR_MESSAGE,
                    reason = e
                )
            )
        }
    }

    suspend fun start(
        activity: Activity,
        orderId: String
    ): VenmoStartResult {
        require(orderId.isNotBlank()) { "Order ID cannot be blank" }

        val ccoUpdateResult = ccoAPI.updateClientConfig(
            tokenId = orderId,
            fundingSource = "venmo"
        )

        if (ccoUpdateResult is UpdateClientConfigResult.Failure) {
            val error = PayPalSDKError(
                code = PayPalSDKErrorCode.CHECKOUT_ERROR.ordinal,
                errorDescription = ccoUpdateResult.error.message ?: "Client config update failed"
            )
            return VenmoStartResult.Failure(error)
        }

        val appSwitchUri = coreConfig.environment.venmoBaseUrl.toUri()
            .buildUpon()
            .appendQueryParameter(CHANNEL_PARAM, CHANNEL_VALUE)
            .appendQueryParameter(ENV_PARAM, coreConfig.environment.venmoEnvironment)
            .appendQueryParameter(TOKEN_PARAM, orderId)
            .build()

        return try {
            val cctOptions = ChromeCustomTabOptions(launchUri = appSwitchUri)
            when (chromeCustomTabsClient.launch(activity, cctOptions)) {
                LaunchChromeCustomTabResult.Success -> VenmoStartResult.Success
                LaunchChromeCustomTabResult.ActivityNotFound -> {
                    val error = PayPalSDKError(
                        code = PayPalSDKErrorCode.CHECKOUT_ERROR.ordinal,
                        errorDescription = "Unable to launch Venmo app or web browser"
                    )
                    VenmoStartResult.Failure(error)
                }
            }
        } catch (e: Exception) {
            val error = PayPalSDKError(
                code = PayPalSDKErrorCode.CHECKOUT_ERROR.ordinal,
                errorDescription = e.message ?: "Failed to launch Venmo"
            )
            VenmoStartResult.Failure(error)
        }
    }

    fun finishStart(intent: Intent): VenmoFinishStartResult? {
        // Get deep link URI from intent
        val deepLinkUri = intent.data ?: return null

        // Parse Venmo result from deep link parameters
        // App link verification is handled by Android's autoVerify mechanism
        val orderId = deepLinkUri.getQueryParameter(PAGE_URL_PARAM).orEmpty()
        val canceled = deepLinkUri.getQueryParameter(CANCELED_PARAM)
        val approved = deepLinkUri.getQueryParameter(APPROVED_PARAM)

        return when {
            canceled.toBoolean() || !approved.toBoolean() -> {
                VenmoFinishStartResult.Failure(
                    PayPalSDKError(
                        code = PayPalSDKErrorCode.CHECKOUT_ERROR.ordinal,
                        errorDescription = CANCELLATION_MESSAGE
                    )
                )
            }

            approved.toBoolean() -> {
                val payerId = deepLinkUri.getQueryParameter(PAYER_ID_PARAM)
                if (payerId.isNullOrEmpty()) {
                    VenmoFinishStartResult.Failure(
                        PayPalSDKError(
                            code = PayPalSDKErrorCode.DATA_PARSING_ERROR.ordinal,
                            errorDescription = "$MISSING_PARAMS_MESSAGE: payerId"
                        )
                    )
                } else {
                    VenmoFinishStartResult.Success(orderId, payerId, true)
                }
            }

            else -> {
                VenmoFinishStartResult.Failure(
                    PayPalSDKError(
                        code = PayPalSDKErrorCode.DATA_PARSING_ERROR.ordinal,
                        errorDescription = "$INVALID_APPROVAL_MESSAGE: $approved"
                    )
                )
            }
        }
    }

}
