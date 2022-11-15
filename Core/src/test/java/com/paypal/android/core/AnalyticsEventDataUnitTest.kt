package com.paypal.android.core

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class AnalyticsEventDataUnitTest {

    @Test
    fun `it should properly format all analytics metadata`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sut = AnalyticsEventData("fake-event", context, "fake-session-id")

        // language=JSON
        val expected = """
        {
            "events": {
                "event_params": {
                    "app_id": "com.braintree.uber",
                    "app_name": "Uber",
                    "c_sdk_ver": "5.12.0",
                    "client_id": "172B29EC22C84E9884029448B4B95A71",
                    "client_os": "iOS 13.7",
                    "comp": "ppmobilesdk",
                    "device_manufacturer": "Apple",
                    "event_name": "fake-event",
                    "event_source": "mobile-native",
                    "ios_package_manager": "Swift Package Manager",
                    "is_simulator": true,
                    "mapv": "7.11.0",
                    "merchant_id": "21705010674",
                    "mobile_device_model": "iPhone11,8",
                    "platform": "iOS",
                    "session_id": "fake-session-id",
                    "t": "1666816838453",
                    "tenant_name": "paypal"
                }
            }
        }       
        """

        JSONAssert.assertEquals(JSONObject(expected), sut.toJSON(), false)
    }
}