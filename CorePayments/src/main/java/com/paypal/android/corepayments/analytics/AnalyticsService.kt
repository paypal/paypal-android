package com.paypal.android.corepayments.analytics

import android.content.Context
import android.util.Log
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.RestClient
import com.paypal.android.corepayments.SecureTokenServiceAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.UUID

class AnalyticsService internal constructor(
    private val deviceInspector: DeviceInspector,
    private val environment: Environment,
    private val restClient: RestClient,
    private val secureTokenServiceAPI: SecureTokenServiceAPI
) {

    constructor(context: Context, coreConfig: CoreConfig) :
            this(
                DeviceInspector(context),
                coreConfig.environment,
                RestClient(coreConfig),
                SecureTokenServiceAPI(coreConfig)
            )

    fun sendAnalyticsEvent(name: String) = GlobalScope.launch {
        val timestamp = System.currentTimeMillis()

        try {
            val clientID = secureTokenServiceAPI.fetchCachedOrRemoteClientID()
            val analyticsEventData = AnalyticsEventData(
                clientID,
                environment.name.lowercase(),
                name,
                timestamp,
                sessionId,
                deviceInspector.inspect()
            )
            val apiRequest = createHttpRequestForAnalytics(analyticsEventData)
            val response = restClient.send(apiRequest)
            if (!response.isSuccessful) {
                Log.d("[PayPal SDK]", "Failed to send analytics: ${response.error?.message}")
            }
        } catch (e: PayPalSDKError) {
            Log.d(
                "[PayPal SDK]",
                "Failed to send analytics due to missing clientID: ${e.message}"
            )
        }
    }

    private fun createHttpRequestForAnalytics(analyticsEventData: AnalyticsEventData): APIRequest {
        val body = analyticsEventData.toJSON().toString()
        return APIRequest("v1/tracking/events", HttpMethod.POST, body)
    }

    companion object {
        private val sessionId = UUID.randomUUID().toString()
    }
}
