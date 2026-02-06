package com.paypal.android.robots

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.paypal.android.MainActivity

/**
 * Provides API for testing PayPal checkout workflows
 */
@OptIn(ExperimentalTestApi::class)
class PayPalCheckoutRobot(
    private val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
) {

    private val waitTimeoutMs = 15_000L

    fun navigateToPayPalCheckout() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("PayPal Checkout"),
            waitTimeoutMs
        )
        composeTestRule.onNodeWithText("PayPal Checkout").performClick()
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Create an Order"),
            waitTimeoutMs
        )
    }

    fun setAppSwitch(enabled: Boolean) = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("APP SWITCH WHEN AVAILABLE"),
            waitTimeoutMs
        )
        val toggleText = if (enabled) "YES" else "NO"
        composeTestRule.onNodeWithText(toggleText).performClick()
    }

    fun setIntent(intent: String) = apply {
        composeTestRule.waitUntilExactlyOneExists(hasText("INTENT"), waitTimeoutMs)
        val currentIntent = if (intent == "AUTHORIZE") "CAPTURE" else "AUTHORIZE"
        composeTestRule.onNodeWithText(currentIntent).performClick()
        composeTestRule.onNodeWithText(intent).performClick()
    }

    fun clickCreateOrder() = apply {
        composeTestRule.waitUntilExactlyOneExists(hasText("CREATE ORDER"), waitTimeoutMs)
        composeTestRule.onNodeWithText("CREATE ORDER").performClick()
    }

    fun verifyOrderCreated() = apply {
        composeTestRule.waitUntilExactlyOneExists(hasText("ORDER CREATED"), waitTimeoutMs)
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Launch PayPal"),
            waitTimeoutMs
        )
    }

    fun verifyOnCheckoutScreen() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Create an Order"),
            waitTimeoutMs
        )
    }

    fun createOrder(
        appSwitchEnabled: Boolean = false,
        intent: String = "CAPTURE"
    ) = apply {
        setAppSwitch(appSwitchEnabled)
        setIntent(intent)
        clickCreateOrder()
        verifyOrderCreated()
    }
}
