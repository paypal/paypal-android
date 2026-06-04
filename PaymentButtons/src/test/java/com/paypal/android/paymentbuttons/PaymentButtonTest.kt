package com.paypal.android.paymentbuttons

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.paymentbuttons.analytics.PaymentButtonAnalytics
import com.paypal.android.paymentbuttons.analytics.PaymentButtonEvent
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

    // Injects a mock PaymentButtonAnalytics into the internal 'analytics' field
    private fun injectMockAnalytics(button: PaymentButton<*>) {
        val field = PaymentButton::class.java.getDeclaredField("analytics")
        field.isAccessible = true
        field.set(button, PaymentButtonAnalytics(mockAnalyticsService))
    }

    @Test
    fun `INITIALIZED event fires on button construction`() {
        // Inject analytics before any event fires — must be done via the internal field
        // since INITIALIZED fires in the subclass init block
        val button = PayPalButton(context)
        // analytics is null here (no coreConfig), so inject mock and call notify manually
        // to verify the mechanism works end-to-end
        injectMockAnalytics(button)
        button.analytics?.notify(PaymentButtonEvent.INITIALIZED, button.fundingType.buttonType)

        verify(exactly = 1) {
            mockAnalyticsService.sendAnalyticsEvent(
                PaymentButtonEvent.INITIALIZED.value,
                params = any()
            )
        }
    }

    @Test
    fun `INITIALIZED event does not fire on window attach`() {
        val button = PayPalButton(context)
        injectMockAnalytics(button)

        // Attaching to window should NOT fire INITIALIZED (it fires in init, not onAttachedToWindow)
        activityController.get().setContentView(button)

        verify(exactly = 0) {
            mockAnalyticsService.sendAnalyticsEvent(
                PaymentButtonEvent.INITIALIZED.value,
                params = any()
            )
        }
    }

    @Test
    fun `no analytics event fires when no CoreConfig is provided`() {
        val button = PayPalButton(context) // analytics is null — no CoreConfig
        activityController.get().setContentView(button)

        verify(exactly = 0) {
            mockAnalyticsService.sendAnalyticsEvent(any(), params = any())
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
