package com.paypal.android.core

import android.util.Log

class API internal constructor(
    private val configuration: CoreConfig,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory
) {

    constructor(configuration: CoreConfig) :
            this(configuration, Http(), HttpRequestFactory())

    suspend fun send(apiRequest: APIRequest): HttpResponse {
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        return http.send(httpRequest)
    }

    @Throws(PayPalSDKError::class)
    suspend fun getClientId(): String {
        val apiRequest = APIRequest("v1/oauth2/token", HttpMethod.GET)
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        val response = http.send(httpRequest)
        val correlationID = response.headers["Paypal-Debug-Id"]
        if (response.isSuccessful) {
            return parseClientId(response.body, correlationID)
        }

        throw APIClientError.serverResponseError(correlationID)
    }

    @Throws(PayPalSDKError::class)
    private fun parseClientId(responseBody: String?, correlationID: String?): String {
        if (responseBody.isNullOrBlank()) {
            throw APIClientError.noResponseData(correlationID)
        }
        val json = PaymentsJSON(responseBody)
        val clientID = json.optString("client_id")
        if (clientID.isNullOrBlank()) {
            throw APIClientError.dataParsingError(correlationID)
        }
        return clientID
    }

    suspend fun sendAnalyticsEvent(name: String) {
//        val requestBody = """
//            {
//              "events": {
//                "event_params": {
//                  "event_name": "hello_from_team_sdk2"
//                }
//              }
//            }
//        """.trimIndent()

        val analyticsEventData = AnalyticsEventData("cannillo_app_id").toJSON().toString()


        val apiRequest = APIRequest("v1/tracking/events", HttpMethod.POST, analyticsEventData)
        val httpRequest =
            httpRequestFactory.createHttpRequestForFPTI(apiRequest)
        val response = http.send(httpRequest)
        if (response.isSuccessful) {
            Log.d("FPTI", "SENT SUCCESS")
        }

//        throw APIClientError.serverResponseError(correlationID)


    }
}
