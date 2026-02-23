package com.paypal.android

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.paypal.android.robots.PayPalCheckoutRobot
import com.paypal.android.uishared.enums.ReturnToAppStrategyOption
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PayPalWebCheckoutTest {

    companion object {
        private const val TAG = "PayPalWebCheckoutTest"
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { PayPalCheckoutRobot(composeTestRule) }

    private fun setupReturnToAppStrategyOption(strategy: ReturnToAppStrategyOption) {
        if (strategy == ReturnToAppStrategyOption.APP_LINKS) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val packageName = context.packageName

            // Extract the actual app package name (remove .test suffix for the main app)
            val appPackageName = packageName.removeSuffix(".test")

            setupAppLinks(appPackageName)
        }
    }

    @After
    fun tearDown() {
        // Reset app links after each test to ensure test isolation
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val packageName = context.packageName.removeSuffix(".test")

        resetAppLinks(packageName)
    }

    @Test
    fun shouldCreateOrderWithIntentAndAppSwitch(
        @TestParameter("AUTHORIZE", "CAPTURE") intent: String,
        @TestParameter appSwitchEnabled: Boolean,
        /*
          * Want to run CUSTOM_URL_SCHEME tests first to ensure app links are configured before they are executed
          * since app links configuration takes time, running CUSTOM_URL_SCHEME gives extra time
         */
        @TestParameter(
            "CUSTOM_URL_SCHEME",
            "APP_LINKS"
        ) returnToAppStrategy: ReturnToAppStrategyOption,
        @TestParameter forceLogin: Boolean
    ) {
        Log.d(TAG, "════════════════════════════════════════════════════════════════")
        Log.d(TAG, "🚀 Starting test with parameters:")
        Log.d(TAG, "   Intent: $intent")
        Log.d(TAG, "   App Switch Enabled: $appSwitchEnabled")
        Log.d(TAG, "   Return to App Strategy: $returnToAppStrategy")
        Log.d(TAG, "   Force Login: $forceLogin")
        Log.d(TAG, "════════════════════════════════════════════════════════════════")

        setupReturnToAppStrategyOption(returnToAppStrategy)

        // Clear Chrome cache to force login if requested
        if (forceLogin) {
            clearChromeCache()
        }

        robot
            .navigateToPayPalCheckout()
            .createOrder(
                appSwitchEnabled = appSwitchEnabled,
                intent = intent,
                returnToAppStrategy = returnToAppStrategy
            )
            .startCheckoutWithLogin(TestConfig.TEST_EMAIL, TestConfig.TEST_PASSWORD)
            .completeOrder()

        Log.d(TAG, "✅ Test completed successfully!")
    }
}
