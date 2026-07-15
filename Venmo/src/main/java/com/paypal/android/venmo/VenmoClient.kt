package com.paypal.android.venmo

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.PayPalSDKErrorCode
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.api.GetFundingEligibility
import com.paypal.android.corepayments.browserswitch.ChromeCustomTabOptions
import com.paypal.android.corepayments.browserswitch.ChromeCustomTabsClient
import com.paypal.android.corepayments.browserswitch.LaunchChromeCustomTabResult
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.venmo.analytics.VenmoAnalytics
import com.paypal.android.venmo.analytics.VenmoCheckoutEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VenmoClient internal constructor(
    private val context: Context,
    private val coreConfig: CoreConfig,
    private val ccoAPI: UpdateClientConfigAPI,
    private val getFundingEligibility: GetFundingEligibility,
    private val chromeCustomTabsClient: ChromeCustomTabsClient,
    private val analytics: VenmoAnalytics,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob())
) {

    companion object {
        private const val VENMO = "VENMO"
        private const val CHANNEL_PARAM = "channel"
        private const val CHANNEL_VALUE = "in-app"
        private const val TOKEN_PARAM = "token"

        private const val INELIGIBLE_MESSAGE = "Venmo is not eligible for this transaction"

        private const val PAYER_ID_PARAM = "PayerID"
        private const val APPROVED_PARAM = "approved"
        private const val CANCELED_PARAM = "canceled"

    }

    constructor(context: Context, config: CoreConfig) : this(
        context = context,
        coreConfig = config,
        ccoAPI = UpdateClientConfigAPI(context, config),
        getFundingEligibility = GetFundingEligibility(config),
        analytics = VenmoAnalytics(AnalyticsService(context, config)),
        chromeCustomTabsClient = ChromeCustomTabsClient()
    )

    suspend fun isEligible(buyerCountry: String): VenmoEligibilityResult {
        require(coreConfig.clientId.isNotBlank()) { "Client ID cannot be blank" }
        require(buyerCountry.isNotBlank()) { "Buyer country cannot be blank" }

        return try {
            val eligibilityResult = getFundingEligibility(
                context = this.context,
                clientId = coreConfig.clientId,
                fundingSources = listOf(VENMO),
                merchantIds = listOf(coreConfig.merchantId),
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
                APIClientError.unknownError(throwable = e)
            )
        }
    }

    suspend fun start(
        activity: Activity,
        orderId: String
    ): VenmoStartResult {
        require(orderId.isNotBlank()) { "Order ID cannot be blank" }

        analytics.notify(VenmoCheckoutEvent.START, orderId)

        // Update client config; ignore result as transaction should proceed regardless
        ccoAPI.updateClientConfig(
            tokenId = orderId,
            fundingSource = VENMO
        )

        val appSwitchUri = coreConfig.environment.venmoCheckoutBaseUrl.toUri()
            .buildUpon()
            .appendQueryParameter(CHANNEL_PARAM, CHANNEL_VALUE)
            .appendQueryParameter(TOKEN_PARAM, orderId)
            .build()

        return try {
            val cctOptions = ChromeCustomTabOptions(launchUri = appSwitchUri)
            when (chromeCustomTabsClient.launch(activity, cctOptions)) {
                LaunchChromeCustomTabResult.Success -> {
                    analytics.notify(VenmoCheckoutEvent.LAUNCH_SUCCESS, orderId)
                    VenmoStartResult.Success
                }

                LaunchChromeCustomTabResult.ActivityNotFound -> {
                    analytics.notify(VenmoCheckoutEvent.LAUNCH_FAILED, orderId)
                    val error = PayPalSDKError(
                        code = PayPalSDKErrorCode.CHECKOUT_ERROR.ordinal,
                        errorDescription = "Unable to launch Venmo app or web browser"
                    )
                    VenmoStartResult.Failure(error)
                }
            }
        } catch (e: Exception) {
            analytics.notify(VenmoCheckoutEvent.LAUNCH_FAILED, orderId)
            val error = PayPalSDKError(
                code = PayPalSDKErrorCode.CHECKOUT_ERROR.ordinal,
                errorDescription = e.message ?: "Failed to launch Venmo"
            )
            VenmoStartResult.Failure(error)
        }
    }

    fun finishStart(intent: Intent): VenmoFinishStartResult {
        // Get deep link URI from intent
        val deepLinkUri = intent.data ?: return VenmoFinishStartResult.NoResult
        val orderId = deepLinkUri.getQueryParameter(TOKEN_PARAM).orEmpty()
        val canceled = deepLinkUri.getQueryParameter(CANCELED_PARAM).toBoolean()
        val approved = deepLinkUri.getQueryParameter(APPROVED_PARAM).toBoolean()

        return when {
            canceled -> {
                analytics.notify(VenmoCheckoutEvent.CANCELED, orderId.takeIf { it.isNotEmpty() })
                VenmoFinishStartResult.Canceled(orderId.takeIf { it.isNotEmpty() })
            }

            approved -> {
                val payerId = deepLinkUri.getQueryParameter(PAYER_ID_PARAM)
                if (payerId.isNullOrEmpty()) {
                    analytics.notify(VenmoCheckoutEvent.FAIL, orderId.takeIf { it.isNotEmpty() })
                    VenmoFinishStartResult.Failure(
                        PayPalSDKError(
                            code = PayPalSDKErrorCode.DATA_PARSING_ERROR.ordinal,
                            errorDescription = "Result did not contain the expected data. Payer ID is null."
                        )
                    )
                } else {
                    analytics.notify(VenmoCheckoutEvent.SUCCESS, orderId)
                    VenmoFinishStartResult.Success(orderId, payerId, true)
                }
            }

            else -> {
                VenmoFinishStartResult.Failure(
                    PayPalSDKError(
                        code = PayPalSDKErrorCode.DATA_PARSING_ERROR.ordinal,
                        errorDescription = "Result did not contain valid approval or cancellation status."
                    )
                )
            }
        }
    }

    /**
     * Check Venmo payment eligibility with callback.
     *
     * @param buyerCountry the buyer's country for eligibility determination
     * @param callback callback to receive the eligibility result
     */
    fun isEligible(
        buyerCountry: String,
        callback: VenmoEligibilityCallback
    ) {
        applicationScope.launch {
            val result = isEligible(buyerCountry)
            withContext(Dispatchers.Main) {
                callback.onVenmoEligibilityResult(result)
            }
        }
    }

    /**
     * Initiate Venmo checkout with callback.
     *
     * @param activity the activity to launch Venmo from
     * @param orderId the order ID for the Venmo payment
     * @param callback callback to receive the start result
     */
    fun start(
        activity: Activity,
        orderId: String,
        callback: VenmoStartCallback
    ) {
        applicationScope.launch {
            val result = start(activity, orderId)
            withContext(Dispatchers.Main) {
                callback.onVenmoStartResult(result)
            }
        }
    }

}
