package com.paypal.android.robots

import android.util.Log
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.paypal.android.MainActivity
import com.paypal.android.uishared.enums.DeepLinkStrategy

/**
 * Provides API for testing PayPal checkout workflows
 */
@OptIn(ExperimentalTestApi::class)
class PayPalCheckoutRobot(
    private val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
) {

    private val waitTimeoutMs = 15_000L

    companion object {
        private const val TAG = "PayPalCheckoutRobot"
    }

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
        intent: String = "CAPTURE",
        deepLinkStrategy: DeepLinkStrategy
    ) = apply {
        setAppSwitch(appSwitchEnabled)
        setIntent(intent)
        setDeepLinkStrategy(deepLinkStrategy)
        clickCreateOrder()
        verifyOrderCreated()

        // Verify the "ID" label exists, which indicates OrderView is displayed
        composeTestRule.waitUntilExactlyOneExists(hasText("ID"), waitTimeoutMs)
        Log.d(
            TAG,
            "✅ Order created successfully with intent: $intent, deepLinkStrategy: $deepLinkStrategy"
        )
    }

    fun setDeepLinkStrategy(deepLinkStrategy: DeepLinkStrategy) = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("DEEP LINK STRATEGY"),
            waitTimeoutMs
        )
        val strategyText = when (deepLinkStrategy) {
            DeepLinkStrategy.APP_LINKS -> "APP_LINKS"
            DeepLinkStrategy.CUSTOM_URL_SCHEME -> "CUSTOM_URL_SCHEME"
        }
        composeTestRule.onNodeWithText(strategyText).performClick()
    }

    fun startCheckout() = apply {
        // Wait for Step 2 to appear
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Launch PayPal"),
            waitTimeoutMs
        )

        // Click on "START CHECKOUT" button
        composeTestRule.waitUntilExactlyOneExists(hasText("START CHECKOUT"), waitTimeoutMs)
        composeTestRule.onNodeWithText("START CHECKOUT").performClick()

        // Get UiDevice instance for interacting with Chrome Custom Tab
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Wait for Chrome Custom Tab or PayPal app to open
        // Look for common PayPal web elements like "Review Order", "Pay Now", or "Continue"
        val webTimeout = 30_000L
        val reviewOrderButton = device.wait(
            Until.findObject(By.textContains("Review")),
            webTimeout
        ) ?: device.wait(
            Until.findObject(By.textContains("Continue")),
            webTimeout
        ) ?: device.wait(
            Until.findObject(By.textContains("Pay Now")),
            webTimeout
        )

        if (reviewOrderButton != null) {
            Log.d(TAG, "✅ Found PayPal checkout button in browser")
            reviewOrderButton.click()
            Thread.sleep(2000) // Wait for click to process
        } else {
            Log.w(
                TAG,
                "⚠️ Could not find PayPal checkout button - may be in app switch or sandbox mode"
            )
        }

        // Wait for return to app and checkout completion
        device.wait(Until.hasObject(By.pkg("com.paypal.android")), webTimeout)

        // Wait for checkout to complete and verify success
        composeTestRule.waitUntilExactlyOneExists(hasText("CHECKOUT COMPLETE"), waitTimeoutMs)

        // Verify Order ID and Payer ID labels are present
        composeTestRule.waitUntilExactlyOneExists(hasText("Order ID"), waitTimeoutMs)
        composeTestRule.waitUntilExactlyOneExists(hasText("Payer ID"), waitTimeoutMs)

        Log.d(TAG, "🚀 PayPal checkout started and completed successfully")
    }

    fun completeOrder() = apply {
        // Wait for Step 3 to appear
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Complete Order"),
            waitTimeoutMs
        )

        // Click on "COMPLETE ORDER" button
        composeTestRule.waitUntilExactlyOneExists(hasText("COMPLETE ORDER"), waitTimeoutMs)
        composeTestRule.onNodeWithText("COMPLETE ORDER").performClick()

        // Wait for order completion and verify success
        composeTestRule.waitUntilExactlyOneExists(hasText("ORDER COMPLETED"), waitTimeoutMs)

        Log.d(TAG, "🎉 Order completed successfully - Full PayPal checkout flow finished!")
    }
}
