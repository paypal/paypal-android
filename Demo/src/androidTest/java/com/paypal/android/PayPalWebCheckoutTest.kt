package com.paypal.android

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.paypal.android.robots.PayPalCheckoutRobot
import com.paypal.android.uishared.enums.DeepLinkStrategy
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PayPalWebCheckoutTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { PayPalCheckoutRobot(composeTestRule) }

    private fun setupDeepLinkStrategy(strategy: DeepLinkStrategy) {
        if (strategy == DeepLinkStrategy.APP_LINKS) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val packageName = context.packageName // Gets "com.paypal.android.test" for test APK

            // Extract the actual app package name (remove .test suffix for the main app)
            val appPackageName = packageName.removeSuffix(".test")

            executeShellCommandWithOutput(
                instrumentation,
                "pm set-app-links --package $appPackageName 1 all"
            )

            executeShellCommandWithOutput(
                instrumentation,
                "pm set-app-links-user-selection --package $appPackageName true all"
            )
        }
    }

    @After
    fun tearDown() {
        // Reset app links after each test to ensure test isolation
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val packageName = context.packageName.removeSuffix(".test")

        executeShellCommandWithOutput(
            instrumentation,
            "pm set-app-links --package $packageName 0 all"
        )
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
        setupDeepLinkStrategy(deepLinkStrategy)

        robot
            .navigateToPayPalCheckout()
            .createOrder(
                appSwitchEnabled = appSwitchEnabled,
                intent = intent,
                deepLinkStrategy = deepLinkStrategy
            )
            .startCheckoutWithLogin(TestConfig.TEST_EMAIL, TestConfig.TEST_PASSWORD)
            .completeOrder()
    }
}
