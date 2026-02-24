package com.paypal.android

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.paypal.android.robots.DeviceSettingsRobot
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

    private val checkoutRobot by lazy { PayPalCheckoutRobot(composeTestRule) }
    private val deviceSettingsRobot = DeviceSettingsRobot()

    @After
    fun tearDown() {
        deviceSettingsRobot.resetAppLinksToDefaults()
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

        if (returnToAppStrategy == ReturnToAppStrategyOption.APP_LINKS) {
            deviceSettingsRobot.setupAppLinksForCurrentApp()
        }

        // Clear browser cache to force login if requested
        if (forceLogin) {
            deviceSettingsRobot.clearBrowserCache()
        }

        checkoutRobot
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
