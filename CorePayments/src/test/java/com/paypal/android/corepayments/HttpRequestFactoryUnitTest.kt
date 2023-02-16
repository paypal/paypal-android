package com.paypal.android.corepayments

import com.paypal.android.corepayments.analytics.AnalyticsEventData
import com.paypal.android.corepayments.analytics.DeviceData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.net.URL

class HttpRequestFactoryUnitTest {

    private val configuration = CoreConfig()
    private val requestBody = """{ "sample": "json" }"""

    private lateinit var sut: HttpRequestFactory

    @Before
    fun beforeEach() {
        sut = HttpRequestFactory("sample-language")
    }

    @Test
    fun `it should properly format the url for the sandbox environment`() {
        val apiRequest = APIRequest("sample/path", HttpMethod.GET, null)
        val sandboxConfig = CoreConfig(environment = Environment.SANDBOX)

        val result = sut.createHttpRequestFromAPIRequest(apiRequest, sandboxConfig)
        assertEquals(URL("https://api.sandbox.paypal.com/sample/path"), result.url)
    }

    @Test
    fun `it should properly format the url for the staging environment`() {
        val apiRequest = APIRequest("sample/path", HttpMethod.GET, null)
        val stagingConfig = CoreConfig(environment = Environment.STAGING)

        val result = sut.createHttpRequestFromAPIRequest(apiRequest, stagingConfig)
        assertEquals(URL("https://api.msmaster.qa.paypal.com/sample/path"), result.url)
    }

    @Test
    fun `it should properly format the url for the live environment`() {
        val apiRequest = APIRequest("sample/path", HttpMethod.GET, null)
        val prodConfig = CoreConfig(environment = Environment.LIVE)

        val result = sut.createHttpRequestFromAPIRequest(apiRequest, prodConfig)
        assertEquals(URL("https://api.paypal.com/sample/path"), result.url)
    }

    @Test
    fun `it should forward the http method`() {
        val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
        val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration)

        assertEquals(HttpMethod.POST, result.method)
    }

    @Test
    fun `it should forward the http body`() {
        val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
        val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration)

        assertEquals(requestBody, result.body)
    }

    @Test
    fun `it should set accept encoding default header`() {
        val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
        val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration)

        assertEquals("gzip", result.headers["Accept-Encoding"])
    }

    @Test
    fun `it should set accept language default header`() {
        val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
        val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration)

        assertEquals("sample-language", result.headers["Accept-Language"])
    }

    @Test
    fun `it should add bearer token authorization header`() {
        val mockAccessToken = "mock_access_token"
        val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
        val result = sut.createHttpRequestFromAPIRequest(
            apiRequest,
            CoreConfig(accessToken = mockAccessToken),
        )

        val expected = "Bearer $mockAccessToken"
        assertEquals(expected, result.headers["Authorization"])
    }

    @Test
    fun `it should add content type json header for POST requests`() {
        val apiRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)
        val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration)

        assertEquals("application/json", result.headers["Content-Type"])
    }

    @Test
    fun `it should add content type json header for GET requests`() {
        val apiRequest = APIRequest("sample/path", HttpMethod.GET, requestBody)
        val result = sut.createHttpRequestFromAPIRequest(apiRequest, configuration)

        assertNull(result.headers["Content-Type"])
    }

    @Test
    fun `createHttpRequestForAnalytics properly constructs HTTP request`() {
        val analyticsEventData = AnalyticsEventData(
            clientID = "fake-client-id",
            eventName = "fake-event",
            timestamp = 10000,
            sessionID = "fake-session-id",
            deviceData = DeviceData(
                appId = "fake-app-id",
                appName = "fake-app-name",
                merchantAppVersion = "fake-merchant-app-version",
                clientSDKVersion = "fake-sdk-version",
                deviceManufacturer = "fake-manufacturer",
                deviceModel = "fake-device-model",
                isSimulator = false,
                clientOS = "fake client OS"
            )
        )

        val result = sut.createHttpRequestForAnalytics(analyticsEventData)

        // language=JSON
        val expected = """
            {
                "events": {
                    "event_params": {
                        "app_id": "fake-app-id",
                        "app_name": "fake-app-name",
                        "partner_client_id": "fake-client-id",
                        "c_sdk_ver": "fake-sdk-version",
                        "client_os": "fake client OS",
                        "comp": "ppunifiedsdk",
                        "device_manufacturer": "fake-manufacturer",
                        "event_name": "fake-event",
                        "event_source": "mobile-native",
                        "is_simulator": false,
                        "mapv": "fake-merchant-app-version",
                        "mobile_device_model": "fake-device-model",
                        "platform": "Android",
                        "session_id": "fake-session-id",
                        "t": "10000",
                        "tenant_name": "PayPal"
                    }
                }
            }
            """

        assertEquals(HttpMethod.POST, result.method)
        assertEquals("application/json", result.headers["Content-Type"])
        assertEquals(URL("https://api.paypal.com/v1/tracking/events"), result.url)
        JSONAssert.assertEquals(JSONObject(expected), JSONObject(result.body!!), false)
    }
}
