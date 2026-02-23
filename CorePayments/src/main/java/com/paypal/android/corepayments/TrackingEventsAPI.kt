package com.paypal.android.corepayments

import com.paypal.android.corepayments.analytics.AnalyticsEventData
import com.paypal.android.corepayments.analytics.DeviceData
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class TrackingEventsAPI constructor(
    private val coreConfig: CoreConfig,
    private val restClient: RestClient
) {

    // api-m.sandbox.paypal.com does not currently send FPTI events to BigQuery/Looker
    constructor(coreConfig: CoreConfig) :
            this(coreConfig, RestClient(toLiveConfig(coreConfig)))

    suspend fun sendEvent(event: AnalyticsEventData, deviceData: DeviceData): HttpResponse {
        val apiRequest = createAPIRequestForEvent(event, deviceData)
        return restClient.send(apiRequest)
    }

    @OptIn(InternalSerializationApi::class)
    private fun createAPIRequestForEvent(
        event: AnalyticsEventData,
        deviceData: DeviceData
    ): APIRequest {

        val eventParams = TrackingEventParams(
            appId = deviceData.appId,
            appName = deviceData.appName,
            clientId = coreConfig.clientId,
            clientSDKVersion = deviceData.clientSDKVersion,
            clientOS = deviceData.clientOS,
            component = PPCP_CLIENTS_SDK,
            deviceManufacturer = deviceData.deviceManufacturer,
            deviceModel = deviceData.deviceModel,
            environment = event.environment,
            eventName = event.eventName,
            eventSource = EVENT_SOURCE_MOBILE_NATIVE,
            isSimulator = deviceData.isSimulator,
            merchantAppVersion = deviceData.merchantAppVersion,
            platform = PLATFORM_ANDROID,
            timestamp = event.timestamp.toString(),
            tenantName = TENANT_NAME_PAYPAL,
            orderId = event.orderId,
            buttonType = event.buttonType,
            appSwitchEnabled = event.appSwitchEnabled
        )

        val events = TrackingEvents(eventParams = eventParams)
        val request = TrackingEventRequest(events = events)
        val jsonBody = Json.encodeToString(request)

        return APIRequest("v1/tracking/events", HttpMethod.POST, jsonBody)
    }

    companion object {
        fun toLiveConfig(config: CoreConfig): CoreConfig =
            CoreConfig(config.clientId, environment = Environment.LIVE)

        const val PPCP_CLIENTS_SDK = "ppcpclientsdk"
        const val EVENT_SOURCE_MOBILE_NATIVE = "mobile-native"
        const val PLATFORM_ANDROID = "Android"
        const val TENANT_NAME_PAYPAL = "PayPal"
    }
}
