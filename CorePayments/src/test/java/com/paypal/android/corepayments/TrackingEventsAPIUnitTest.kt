package com.paypal.android.corepayments

import com.paypal.android.corepayments.analytics.AnalyticsEventData
import com.paypal.android.corepayments.analytics.DeviceData
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class TrackingEventsAPIUnitTest {

    private val httpSuccessResponse = HttpResponse(200)
    private val deviceData = DeviceData(
        appId = "fake-app-id",
        appName = "fake-app-name",
        merchantAppVersion = "fake-merchant-app-version",
        clientSDKVersion = "fake-sdk-version",
        deviceManufacturer = "fake-manufacturer",
        deviceModel = "fake-device-model",
        isSimulator = true,
        clientOS = "fake client OS"
    )

    private val coreConfig = CoreConfig("fake-client-id", Environment.SANDBOX)

    private lateinit var restClient: RestClient
    private lateinit var apiRequestSlot: CapturingSlot<APIRequest>

    private lateinit var sut: TrackingEventsAPI

    @Before
    fun beforeEach() {
        restClient = mockk(relaxed = true)

        apiRequestSlot = slot()
        sut = TrackingEventsAPI(coreConfig, restClient)
    }

    @Test
    fun `sendEvent() should send an API request to the tracking API`() = runTest {
        coEvery { restClient.send(capture(apiRequestSlot)) } returns httpSuccessResponse

        val event = AnalyticsEventData(
            environment = "fake-environment",
            eventName = "fake-event",
            timestamp = 123L,
            orderId = "fake-order-id",
            buttonType = "paypal"
        )
        sut.sendEvent(event, deviceData)

        val apiRequest = apiRequestSlot.captured
        assertEquals("v1/tracking/events", apiRequest.path)
        assertEquals(HttpMethod.POST, apiRequest.method)

        // language=JSON
        val expectedBody = """
            {
                "events": {
                    "event_params": {
                        "app_id": "fake-app-id",
                        "app_name": "fake-app-name",
                        "partner_client_id": "fake-client-id",
                        "c_sdk_ver": "fake-sdk-version",
                        "client_os": "fake client OS",
                        "comp": "ppcpclientsdk",
                        "device_manufacturer": "fake-manufacturer",
                        "merchant_sdk_env": "fake-environment",
                        "event_name": "fake-event",
                        "event_source": "mobile-native",
                        "is_simulator": true,
                        "mapv": "fake-merchant-app-version",
                        "mobile_device_model": "fake-device-model",
                        "button_type": "paypal",
                        "platform": "Android",
                        "order_id": "fake-order-id",
                        "t": "123",
                        "tenant_name": "PayPal"
                    }
                }
            }
            """

        val actualBody = apiRequest.body!!
        JSONAssert.assertEquals(JSONObject(expectedBody), JSONObject(actualBody), false)
    }
}
