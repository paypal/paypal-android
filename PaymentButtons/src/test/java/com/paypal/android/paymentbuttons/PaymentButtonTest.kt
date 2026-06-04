package com.paypal.android.paymentbuttons

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.paymentbuttons.analytics.PaymentButtonAnalytics
import com.paypal.android.paymentbuttons.analytics.PaymentButtonEvent
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class PaymentButtonTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var activityController: ActivityController<Activity>
    private lateinit var mockAnalyticsService: AnalyticsService

    @Before
    fun setUp() {
        mockAnalyticsService = mockk(relaxed = true)
        activityController = Robolectric.buildActivity(Activity::class.java).setup()
    }

    @After
    fun tearDown() {
        activityController.destroy()
    }

    private fun injectMockAnalytics(button: PaymentButton<*>) {
        val analyticsField = PaymentButton::class.java.getDeclaredField("analytics")
        analyticsField.isAccessible = true
        analyticsField.set(button, PaymentButtonAnalytics(mockAnalyticsService))

        // Also reset the guard so the injected analytics can fire INITIALIZED
        val guardField = PaymentButton::class.java.getDeclaredField("hasNotifiedInitialized")
        guardField.isAccessible = true
        guardField.set(button, false)
    }

    @Test
    fun `INITIALIZED event fires when button is attached to window`() {
        val button = PayPalButton(context)
        injectMockAnalytics(button)
        activityController.get().setContentView(button)

        verify(exactly = 1) {
            mockAnalyticsService.sendAnalyticsEvent(
                PaymentButtonEvent.INITIALIZED.value,
                params = any()
            )
        }
    }

    @Test
    fun `no analytics event fires when no CoreConfig is provided`() {
        val button = PayPalButton(context) // coreConfig = null, analytics is null
        activityController.get().setContentView(button)

        verify(exactly = 0) {
            mockAnalyticsService.sendAnalyticsEvent(any(), params = any())
        }
    }

    @Test
    fun `INITIALIZED event does not fire again on re-attach`() {
        val button = PayPalButton(context)
        injectMockAnalytics(button)
        val activity = activityController.get()

        activity.setContentView(button)
        clearMocks(mockAnalyticsService, answers = false)

        (button.parent as? android.view.ViewGroup)?.removeView(button)
        activity.setContentView(button)

        verify(exactly = 0) {
            mockAnalyticsService.sendAnalyticsEvent(
                PaymentButtonEvent.INITIALIZED.value,
                params = any()
            )
        }
    }

    @Test
    fun `TAPPED event fires when button is clicked`() {
        val button = PayPalButton(context)
        injectMockAnalytics(button)
        activityController.get().setContentView(button)

        button.setOnClickListener { }
        button.performClick()

        verify(exactly = 1) {
            mockAnalyticsService.sendAnalyticsEvent(
                PaymentButtonEvent.TAPPED.value,
                params = any()
            )
        }
    }
}
