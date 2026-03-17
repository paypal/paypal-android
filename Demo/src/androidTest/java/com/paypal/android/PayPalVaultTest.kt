package com.paypal.android

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.paypal.android.robots.DemoRobot
import com.paypal.android.robots.DeviceSettingsRobot
import com.paypal.android.uishared.enums.ReturnToAppStrategyOption
import com.paypal.android.utils.TestConfig
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PayPalVaultTest {

    companion object {
        private const val TAG = "PayPalVaultTest"
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { DemoRobot(composeTestRule) }

    private val deviceSettingsRobot = DeviceSettingsRobot()

    @Before
    fun setUp() {
        deviceSettingsRobot.disablePasswordManagers()
        deviceSettingsRobot.resetAppLinksToDefaults()
    }

    @After
    fun tearDown() {
        deviceSettingsRobot.resetAppLinksToDefaults()
    }

    @Test
    fun shouldVaultWith(
        @TestParameter appSwitchEnabled: Boolean,
        /*
          * Want to run CUSTOM_URL_SCHEME tests first to ensure app links are configured before they are executed
          * since app links configuration takes time, running CUSTOM_URL_SCHEME gives extra time
         */
        @TestParameter(
            "CUSTOM_URL_SCHEME",
            "APP_LINKS"
        ) returnToAppStrategy: ReturnToAppStrategyOption
    ) {
        if (returnToAppStrategy == ReturnToAppStrategyOption.APP_LINKS) {
            deviceSettingsRobot.setupAppLinksForCurrentApp()
        }

        robot
            .navigateToPayPalVault()
            .vaultWithAppSwitch(
                appSwitchEnabled = appSwitchEnabled,
                returnToAppStrategy = returnToAppStrategy
            )
            .startVaultWithLogin(TestConfig.TEST_EMAIL, TestConfig.TEST_PASSWORD)
            .createPaymentToken()

        Log.d(TAG, "✅ Test completed successfully!")
    }
}
