package com.paypal.android.paymentbuttons

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.analytics.AnalyticsServiceRegistry
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
import android.app.Activity

@RunWith(RobolectricTestRunner::class)
class PaymentButtonTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var activityController: ActivityController<Activity>
    private lateinit var mockAnalyticsService: AnalyticsService

    @Before
    fun setUp() {
        mockAnalyticsService = mockk(relaxed = true)
        activityController = Robolectric.buildActivity(Activity::class.java).setup()

        // Inject the mock service into the registry before each test
        val field = AnalyticsServiceRegistry::class.java.getDeclaredField("_service")
        field.isAccessible = true
        field.set(AnalyticsServiceRegistry, mockAnalyticsService)
    }

    @After
    fun tearDown() {
        val field = AnalyticsServiceRegistry::class.java.getDeclaredField("_service")
        field.isAccessible = true
        field.set(AnalyticsServiceRegistry, null)
        activityController.destroy()
    }

    @Test
    fun `analytics is bound from registry when button is attached to window`() {
        val button = PayPalButton(context)
        activityController.get().setContentView(button)

        verify(exactly = 1) {
            mockAnalyticsService.sendAnalyticsEvent(
                PaymentButtonEvent.INITIALIZED.value,
                buttonType = any()
            )
        }
    }

    @Test
    fun `INITIALIZED event fires once on first attach`() {
        val button = PayPalButton(context)
        activityController.get().setContentView(button)

        verify(exactly = 1) {
            mockAnalyticsService.sendAnalyticsEvent(
                PaymentButtonEvent.INITIALIZED.value,
                buttonType = any()
            )
        }
    }

    @Test
    fun `INITIALIZED event does not fire again on re-attach`() {
        val button = PayPalButton(context)
        val activity = activityController.get()

        // First attach — INITIALIZED fires, analytics is bound
        activity.setContentView(button)

        // Reset recorded calls so we can verify no new ones on the second attach
        clearMocks(mockAnalyticsService, answers = false)

        // Re-attach
        (button.parent as? android.view.ViewGroup)?.removeView(button)
        activity.setContentView(button)

        // No new INITIALIZED event should fire after re-attach
        verify(exactly = 0) {
            mockAnalyticsService.sendAnalyticsEvent(
                PaymentButtonEvent.INITIALIZED.value,
                buttonType = any()
            )
        }
    }

    @Test
    fun `TAPPED event fires when button is clicked`() {
        val button = PayPalButton(context)
        activityController.get().setContentView(button)

        button.setOnClickListener { }
        button.performClick()

        verify(exactly = 1) {
            mockAnalyticsService.sendAnalyticsEvent(
                PaymentButtonEvent.TAPPED.value,
                buttonType = any()
            )
        }
    }
}
