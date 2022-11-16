package com.paypal.android.core

import android.content.Context
import com.paypal.android.core.analytics.DeviceInspector
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceInspectorUnitTest {

    @Test
    fun `it properly inspects context and build info to construct DeviceData`() {
        val mockContext = mockk<Context>()
        val sut = DeviceInspector(mockContext)

        val result = sut.inspect()

        // TODO: - Implement these asserts with either regex or appropriate hard coded actual cases. See iOS AnalyticsEventRequest_Tests for reference.
        assertEquals(result.appId, "")
        assertEquals(result.appName, "")
        assertEquals(result.clientOS, "")
        assertTrue(result.clientSDKVersion.matches(Regex("^\\d+\\.\\d+\\.\\d+(-[0-9a-zA-Z-]+)?$")))
        assertEquals(result.deviceManufacturer, "")
        assertEquals(result.deviceModel, "")
        assertEquals(result.isSimulator, "")
        assertEquals(result.merchantAppVersion, "")
    }
}