package com.paypal.android.corepayments

import com.paypal.android.corepayments.analytics.AnalyticsEventData
import com.paypal.android.corepayments.analytics.DeviceData
import com.paypal.android.corepayments.api.AuthenticationSecureTokenServiceAPI
import com.paypal.android.corepayments.common.Headers
import com.paypal.android.corepayments.model.APIResult
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

internal class TrackingEventsAPI(
    private val coreConfig: CoreConfig,
    private val restClient: RestClient,
    private val authenticationSecureTokenServiceAPI: AuthenticationSecureTokenServiceAPI
) {

    // api-m.sandbox.paypal.com does not currently send FPTI events to BigQuery/Looker
    constructor(coreConfig: CoreConfig) :
            this(
                coreConfig,
                RestClient(coreConfig),
                AuthenticationSecureTokenServiceAPI(coreConfig)
            )

    suspend fun sendEvent(event: AnalyticsEventData, deviceData: DeviceData): HttpResponse {
        val lsatResult = authenticationSecureTokenServiceAPI.createLowScopedAccessToken()
        when (lsatResult) {
            is APIResult.Success -> {
//                val lsat = lsatResult.data
                val lsat = "eyJraWQiOiJjMDg0YjA0NDQwMjE0YzFkYTQ1ZDgwNDE1YjJlZmI5MiIsInR5cCI6IkpXVCIsImFsZyI6IkVTMjU2In0.eyJpc3MiOiJodHRwczovL2FwaS5zYW5kYm94LnBheXBhbC5jb20iLCJzdWIiOiJWOVlQMjdIRk5HMkxXIiwiYWNyIjpbImNsaWVudCJdLCJzY29wZSI6WyJCcmFpbnRyZWU6VmF1bHQiXSwib3B0aW9ucyI6e30sImF6IjoiY2NnMTguc2xjIiwiZXh0ZXJuYWxfaWQiOlsiUGF5UGFsOlY5WVAyN0hGTkcyTFciLCJCcmFpbnRyZWU6dmZxMmI1a3MybW05M3NuZyJdLCJleHAiOjE3NzMzMzQzOTQsImlhdCI6MTc3MzMzMzQ5NCwianRpIjoiVTJBQUxQQm1RWnBkREhfcUMtWFpBQ0YxXzNnblc4U1B4MldtaF9lWDUyc3Z3TkdHa2dOTVRORkIxZHJoelM2UmJETVJiZU9yWVNtMkVVVWNsU0w5ZHZ5VE5uWlhMenAyQ3ZhT1RvcFFGamw4dElLb0NRQXpDOE5XZFd1bks1THciLCJjbGllbnRfaWQiOiJBUVRmdzJpckZmZW1vLWVXRzRINVVZLWI5YXVLaWhVcFhRMkVuZ2w0RzFFc0hKZTJta3BmVXZfU04zTWJhMHYzQ2ZyTDZGa19lY3d2OUVPbyJ9.qAu4_ee3Si42iBZY9aagS7JbH9lCR2-bjY3IDEfFbjSgnJC_t_kFqWNqrtScaWRMI5mWJl6e8SRuMy1UZtTo_w"
                val apiRequest = createAPIRequestForEvent(event, deviceData, lsat)
                return restClient.send(apiRequest)
            }

            is APIResult.Failure -> TODO("HANDLE LSAT FETCH ERROR")
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun createAPIRequestForEvent(
        event: AnalyticsEventData,
        deviceData: DeviceData,
        accessToken: String
    ): APIRequest {

        val commonEventParams = CommonEventParams(
            tenantName = TENANT_NAME_PAYPAL,
            eventSource = EVENT_SOURCE_MOBILE_NATIVE,
            params = SdkCommonTrackingParams(
                appId = deviceData.appId,
                appName = deviceData.appName,
                clientId = coreConfig.clientId,
                clientSDKVersion = deviceData.clientSDKVersion,
                clientOS = deviceData.clientOS,
                deviceManufacturer = deviceData.deviceManufacturer,
                deviceModel = deviceData.deviceModel,
                environment = event.environment,
                isSimulator = deviceData.isSimulator,
                merchantAppVersion = deviceData.merchantAppVersion,
                platform = PLATFORM_ANDROID
            )
        )

        val eventParamsV4 = EventParams(
            eventName = event.eventName,
            eventTime = event.timestamp.toString(),
            clientEventKey = UUID.randomUUID().toString(),
            component = PPCP_CLIENTS_SDK,
            // TODO: figure out what this is and why we need it
            spaceKey = "paypal_mobile_sdk",
            params = SdkEventParams(
                orderId = event.orderId,
                buttonType = event.buttonType,
                appSwitchEnabled = event.appSwitchEnabled
            )
        )

        val userEvent = UserEvent(
            // NOTE: empty for now; we aren't passing sessionId, etc. and these fields aren't required
            user = User(),
            commonEventParams = commonEventParams,
            eventParams = eventParamsV4
        )
        val requestV4 = TrackingEventRequestV4(
            requestUUID = UUID.randomUUID().toString(),
            userEvents = listOf(userEvent)
        )

        val jsonBody = Json.encodeToString(requestV4)
        val headers = mapOf(Headers.AUTHORIZATION to "Bearer $accessToken")
        return APIRequest("v4/tracking/events", HttpMethod.POST, jsonBody, headers)

//        val eventParams = TrackingEventParams(
//            appId = deviceData.appId,
//            appName = deviceData.appName,
//            clientId = coreConfig.clientId,
//            clientSDKVersion = deviceData.clientSDKVersion,
//            clientOS = deviceData.clientOS,
//            component = PPCP_CLIENTS_SDK,
//            deviceManufacturer = deviceData.deviceManufacturer,
//            deviceModel = deviceData.deviceModel,
//            environment = event.environment,
//            eventName = event.eventName,
//            eventSource = EVENT_SOURCE_MOBILE_NATIVE,
//            isSimulator = deviceData.isSimulator,
//            merchantAppVersion = deviceData.merchantAppVersion,
//            platform = PLATFORM_ANDROID,
//            timestamp = event.timestamp.toString(),
//            tenantName = TENANT_NAME_PAYPAL,
//            orderId = event.orderId,
//            buttonType = event.buttonType,
//            appSwitchEnabled = event.appSwitchEnabled
//        )
//
//        val events = TrackingEvents(eventParams = eventParams)
//        val request = TrackingEventRequest(events = events)
//        val jsonBody = Json.encodeToString(request)
//
//        return APIRequest("v1/tracking/events", HttpMethod.POST, jsonBody)
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
