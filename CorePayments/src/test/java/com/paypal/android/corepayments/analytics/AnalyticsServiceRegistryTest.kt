package com.paypal.android.corepayments.analytics

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnalyticsServiceRegistryTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val coreConfig = CoreConfig("fake-client-id", Environment.SANDBOX)

    @Before
    @After
    fun resetRegistry() {
        val field = AnalyticsServiceRegistry::class.java.getDeclaredField("_service")
        field.isAccessible = true
        field.set(AnalyticsServiceRegistry, null)
    }

    @Test
    fun `initialize creates and stores an AnalyticsService`() {
        AnalyticsServiceRegistry.initialize(context, coreConfig)
        assertNotNull(AnalyticsServiceRegistry.service)
    }

    @Test
    fun `initialize is a no-op when called a second time`() {
        AnalyticsServiceRegistry.initialize(context, coreConfig)
        val firstService = AnalyticsServiceRegistry.service

        val secondConfig = CoreConfig("different-client-id", Environment.LIVE)
        AnalyticsServiceRegistry.initialize(context, secondConfig)

        assertSame("Second initialize should not replace the first service",
            firstService, AnalyticsServiceRegistry.service)
    }

    @Test
    fun `service returns the same instance on every call`() {
        AnalyticsServiceRegistry.initialize(context, coreConfig)
        val first = AnalyticsServiceRegistry.service
        val second = AnalyticsServiceRegistry.service
        assertSame(first, second)
    }
}
