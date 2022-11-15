package com.paypal.android.core

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.core.analytics.AnalyticsEventData
import com.paypal.android.core.analytics.models.DeviceData
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class AnalyticsEventDataUnitTest {

    @Test
    fun `it should properly format all analytics metadata`() {
        val sut = AnalyticsEventData("fake-event", "fake-session-id", DeviceData(
            "fake-app-name", "fake-app-id", false, "fake-merchant-app-version", ),
        timestamp = 10000)

        // language=JSON
        val expected = """
        {
            "events": {
                "event_params": {
                    "app_id": "fake-app-id",
                    "app_name": "fake-app-name",
                    "c_sdk_ver": "${BuildConfig.PAYPAL_SDK_VERSION}",
                    "client_os": "Android API ${Build.VERSION.SDK_INT}",
                    "comp": "ppunifiedsdk",
                    "device_manufacturer": "${Build.MANUFACTURER}",
                    "event_name": "fake-event",
                    "event_source": "mobile-native",
                    "is_simulator": false,
                    "mapv": "fake-merchant-app-version",
                    "mobile_device_model": "${Build.MODEL}",
                    "platform": "Android",
                    "session_id": "fake-session-id",
                    "t": "10000",
                    "tenant_name": "PayPal"
                }
            }
        }       
        """

        JSONAssert.assertEquals(JSONObject(expected), sut.toJSON(), false)
    }
}
