package com.paypal.android

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.paypal.android.robots.PayPalCheckoutRobot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PayPalWebCheckoutTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { PayPalCheckoutRobot(composeTestRule) }

    @Test
    fun shouldNavigateToPayPalCheckoutScreen() {
        robot
            .navigateToPayPalCheckout()
            .verifyOnCheckoutScreen()
    }

    @Test
    fun shouldCreateOrderWithIntentAndAppSwitch(
        @TestParameter("AUTHORIZE", "CAPTURE") intent: String,
        @TestParameter appSwitchEnabled: Boolean
    ) {
        robot
            .navigateToPayPalCheckout()
            .createOrder(
                appSwitchEnabled = appSwitchEnabled,
                intent = intent
            )
    }
}
