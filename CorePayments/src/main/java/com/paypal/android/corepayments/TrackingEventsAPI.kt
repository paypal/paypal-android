package com.paypal.android.corepayments

import com.paypal.android.corepayments.analytics.AnalyticsEventData
import org.json.JSONObject

class TrackingEventsAPI internal constructor(
    private val restClient: RestClient,
) {

    constructor(coreConfig: CoreConfig) : this(RestClient(coreConfig))

    suspend fun sendEvent(event: AnalyticsEventData): HttpResponse {
        val apiRequest = createAPIRequestForEvent(event)
        return restClient.send(apiRequest)
    }

    private fun createAPIRequestForEvent(event: AnalyticsEventData): APIRequest {
        val eventParams = JSONObject()
            .put(KEY_APP_ID, event.appId)
            .put(KEY_APP_NAME, event.appName)
            .put(KEY_CLIENT_ID, event.clientID)
            .put(KEY_CLIENT_SDK_VERSION, event.clientSDKVersion)
            .put(KEY_CLIENT_OS, event.clientOS)
            .put(KEY_COMPONENT, "ppcpmobilesdk")
            .put(KEY_DEVICE_MANUFACTURER, event.deviceManufacturer)
            .put(KEY_DEVICE_MODEL, event.deviceModel)
            .put(KEY_ENVIRONMENT, event.environment)
            .put(KEY_EVENT_NAME, event.eventName)
            .put(KEY_EVENT_SOURCE, "mobile-native")
            .put(KEY_IS_SIMULATOR, event.isSimulator)
            .put(KEY_MERCHANT_APP_VERSION, event.merchantAppVersion)
            .put(KEY_PLATFORM, "Android")
            .put(KEY_SESSION_ID, event.sessionID)
            .put(KEY_TIMESTAMP, event.timestamp.toString())
            .put(KEY_TENANT_NAME, "PayPal")

        val events = JSONObject()
            .put(KEY_EVENT_PARAMETERS, eventParams)

        val jsonBody = JSONObject().put(KEY_EVENTS, events)
        return APIRequest("v1/tracking/events", HttpMethod.POST, jsonBody.toString())
    }

    companion object {
        const val KEY_APP_ID = "app_id"
        const val KEY_APP_NAME = "app_name"
        const val KEY_CLIENT_ID = "partner_client_id"
        const val KEY_CLIENT_SDK_VERSION = "c_sdk_ver"
        const val KEY_CLIENT_OS = "client_os"
        const val KEY_COMPONENT = "comp"
        const val KEY_DEVICE_MANUFACTURER = "device_manufacturer"
        const val KEY_DEVICE_MODEL = "mobile_device_model"
        const val KEY_ENVIRONMENT = "merchant_app_environment"
        const val KEY_EVENT_NAME = "event_name"
        const val KEY_EVENT_SOURCE = "event_source"
        const val KEY_IS_SIMULATOR = "is_simulator"
        const val KEY_MERCHANT_APP_VERSION = "mapv"
        const val KEY_PLATFORM = "platform"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_TIMESTAMP = "t"
        const val KEY_TENANT_NAME = "tenant_name"

        const val KEY_EVENT_PARAMETERS = "event_params"
        const val KEY_EVENTS = "events"
    }

}