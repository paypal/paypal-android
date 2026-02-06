package com.paypal.android

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.paypal.android.robots.PayPalCheckoutRobot
import com.paypal.android.uishared.enums.DeepLinkStrategy
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PayPalWebCheckoutTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { PayPalCheckoutRobot(composeTestRule) }

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        // Grant permission to handle deeplinks during test
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant com.paypal.android android.permission.WRITE_SECURE_SETTINGS"
        ).close()

        // Clear any existing app link verification state
        instrumentation.uiAutomation.executeShellCommand(
            "pm clear-app-links com.paypal.android"
        ).close()

        // Set the app as the default handler for custom URL scheme
        instrumentation.uiAutomation.executeShellCommand(
            "am start -a android.intent.action.VIEW -d \"com.paypal.android.demo://test\""
        ).close()

        // Wait a moment for settings to apply
        Thread.sleep(500)
    }

    @Test
    fun shouldNavigateToPayPalCheckoutScreen() {
        robot
            .navigateToPayPalCheckout()
            .verifyOnCheckoutScreen()
    }

    @Test
    fun shouldCreateOrderWithIntentAndAppSwitch(
        @TestParameter("AUTHORIZE", "CAPTURE") intent: String,
        @TestParameter appSwitchEnabled: Boolean,
        @TestParameter deepLinkStrategy: DeepLinkStrategy
    ) {
        robot
            .navigateToPayPalCheckout()
            .createOrder(
                appSwitchEnabled = appSwitchEnabled,
                intent = intent,
                deepLinkStrategy = deepLinkStrategy
            )
            .startCheckout()
            .completeOrder()
    }
}
