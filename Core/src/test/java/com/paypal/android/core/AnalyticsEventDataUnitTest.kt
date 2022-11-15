package com.paypal.android.core

import androidx.fragment.app.FragmentActivity
import com.paypal.android.core.analytics.AnalyticsEventData
import io.mockk.mockk
import org.junit.Test

class AnalyticsEventDataUnitTest {

    @Test
    fun `it should properly format all analytics metadata`() {
        val sut = AnalyticsEventData(
            "fake-event",
            mockk<FragmentActivity>(),
            "fake-session-id"
        )

        // TODO: - Implement unit test
        // Assert.assertEquals()
    }
}
