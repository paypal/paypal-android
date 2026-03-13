package com.paypal.android.robots

import android.util.Log
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.paypal.android.MainActivity
import com.paypal.android.test.TestConstants.TIMEOUT_LONG_MS
import com.paypal.android.uishared.enums.ReturnToAppStrategyOption

/**
 * Provides API for testing PayPal checkout workflows
 */
@OptIn(ExperimentalTestApi::class)
class DemoRobot(
    private val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
) {

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }
    private val webPageRobot = PayPalWebPageRobot()

    companion object {
        private const val TAG = "PayPalCheckoutRobot"
    }

    private fun waitForAppToReturn() {
        Log.d(TAG, "⏳ Waiting for app to return from browser...")
        val appReturned =
            device.wait(Until.hasObject(By.pkg("com.paypal.android")), TIMEOUT_LONG_MS)
        if (appReturned) {
            Log.d(TAG, "✅ App returned from browser")
        } else {
            Log.w(TAG, "⚠️ Timeout waiting for app to return from browser")
        }
    }

    fun navigateToPayPalCheckout() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("PayPal Checkout"),
            TIMEOUT_LONG_MS
        )
        composeTestRule.onNodeWithText("PayPal Checkout").performClick()
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Create an Order"),
            TIMEOUT_LONG_MS
        )
    }

    fun setAppSwitch(enabled: Boolean) = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("APP SWITCH WHEN AVAILABLE"),
            TIMEOUT_LONG_MS
        )
        val toggleText = if (enabled) "YES" else "NO"
        composeTestRule.onNodeWithText(toggleText).performClick()
    }

    fun setIntent(intent: String) = apply {
        composeTestRule.waitUntilExactlyOneExists(hasText("INTENT"), TIMEOUT_LONG_MS)
        val currentIntent = if (intent == "AUTHORIZE") "CAPTURE" else "AUTHORIZE"
        composeTestRule.onNodeWithText(currentIntent).performClick()
        composeTestRule.onNodeWithText(intent).performClick()
    }

    fun clickCreateOrder() = apply {
        composeTestRule.waitUntilExactlyOneExists(hasText("CREATE ORDER"), TIMEOUT_LONG_MS)
        composeTestRule.onNodeWithText("CREATE ORDER").performClick()
    }

    fun verifyOrderCreated() = apply {
        composeTestRule.waitUntilExactlyOneExists(hasText("ORDER CREATED"), TIMEOUT_LONG_MS)
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Launch PayPal"),
            TIMEOUT_LONG_MS
        )
    }

    fun createOrder(
        appSwitchEnabled: Boolean,
        intent: String,
        returnToAppStrategy: ReturnToAppStrategyOption
    ) = apply {
        setAppSwitch(appSwitchEnabled)
        setIntent(intent)
        setReturnToAppStrategyOption(returnToAppStrategy)
        clickCreateOrder()
        verifyOrderCreated()

        // Verify the "ID" label exists, which indicates OrderView is displayed
        composeTestRule.waitUntilExactlyOneExists(hasText("ID"), TIMEOUT_LONG_MS)
        Log.d(
            TAG,
            "✅ Order created successfully with intent: $intent, returnToAppStrategy: $returnToAppStrategy"
        )
    }

    fun setReturnToAppStrategyOption(returnToAppStrategy: ReturnToAppStrategyOption) = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("RETURN TO APP STRATEGY"),
            TIMEOUT_LONG_MS
        )
        val strategyText = when (returnToAppStrategy) {
            ReturnToAppStrategyOption.APP_LINKS -> "APP_LINKS"
            ReturnToAppStrategyOption.CUSTOM_URL_SCHEME -> "CUSTOM_URL_SCHEME"
        }
        composeTestRule.onNodeWithText(strategyText).performClick()
    }

    fun startCheckoutWithLogin(email: String, password: String) = apply {
        // Wait for Step 2 to appear
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Launch PayPal"),
            TIMEOUT_LONG_MS
        )

        // Click on "START CHECKOUT" button
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("START CHECKOUT"))

        composeTestRule
            .waitUntilExactlyOneExists(hasText("START CHECKOUT"), TIMEOUT_LONG_MS)
        composeTestRule.onNodeWithText("START CHECKOUT").performClick()

        // Delegate to web page robot for login
        Log.d(TAG, "🔐 Entering PayPal credentials...")
        webPageRobot.checkout(email, password)

        // Wait for return to app and checkout completion
        waitForAppToReturn()

        composeTestRule.waitUntilExactlyOneExists(hasText("CHECKOUT COMPLETE"), TIMEOUT_LONG_MS)

        // Verify Order ID and Payer ID labels are present
        composeTestRule.waitUntilExactlyOneExists(hasText("Order ID"))
        composeTestRule.waitUntilExactlyOneExists(hasText("Payer ID"))

        Log.d(TAG, "🚀 PayPal checkout with login completed successfully")
    }

    fun completeOrder() = apply {
        // Wait for Step 3 to appear
        composeTestRule.waitUntilExactlyOneExists(hasText("Complete Order"))

        // Click on "COMPLETE ORDER" button
        composeTestRule.waitUntilExactlyOneExists(hasText("COMPLETE ORDER"))
        composeTestRule.onNodeWithText("COMPLETE ORDER").performClick()

        // Wait for order completion and verify success
        composeTestRule.waitUntilExactlyOneExists(hasText("ORDER COMPLETED"), TIMEOUT_LONG_MS)

        Log.d(TAG, "🎉 Order completed successfully - Full PayPal checkout flow finished!")
    }

    fun navigateToPayPalVault() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Paypal Vault")
        )
        composeTestRule.onNodeWithText("Paypal Vault").performClick()
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Create Setup Token")
        )
    }

    fun vaultWithAppSwitch(
        appSwitchEnabled: Boolean,
        returnToAppStrategy: ReturnToAppStrategyOption
    ) = apply {
        setAppSwitch(appSwitchEnabled)
        setReturnToAppStrategyOption(returnToAppStrategy)
        clickCreateSetupToken()
        verifySetupTokenCreated()

        Log.d(
            TAG,
            "✅ Setup token created successfully with appSwitch: $appSwitchEnabled, " +
                    "returnToAppStrategy: $returnToAppStrategy"
        )
    }

    fun clickCreateSetupToken() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("CREATE SETUP TOKEN"),
            TIMEOUT_LONG_MS
        )
        composeTestRule.onNodeWithText("CREATE SETUP TOKEN").performClick()
    }

    fun verifySetupTokenCreated() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("SETUP TOKEN CREATED"),
            TIMEOUT_LONG_MS
        )
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Vault PayPal")
        )
    }

    fun startVaultWithLogin(email: String, password: String) = apply {
        // Wait for Step 2 to appear
        composeTestRule.waitUntilExactlyOneExists(
            hasText("VAULT PAYPAL")
        )

        // Click on "START VAULT" button
        composeTestRule.waitUntilExactlyOneExists(hasText("VAULT PAYPAL"))
        composeTestRule.onNodeWithText("VAULT PAYPAL").performClick()

        webPageRobot.checkout(email, password)

        // Wait for return to app and vault completion
        waitForAppToReturn()

        // Wait for vault to complete and verify success
        composeTestRule.waitUntilExactlyOneExists(hasText("PAYPAL VAULTED"), TIMEOUT_LONG_MS)
        composeTestRule.waitUntilExactlyOneExists(hasText("Approval Session ID"))

        Log.d(TAG, "🚀 PayPal vault with login completed successfully")
    }

    fun createPaymentToken() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("CREATE PAYMENT TOKEN")
        )
        composeTestRule.onNodeWithText("CREATE PAYMENT TOKEN").performClick()
        composeTestRule.waitUntilExactlyOneExists(
            hasText("PAYMENT TOKEN CREATED"),
            TIMEOUT_LONG_MS
        )
        Log.d(TAG, "🎉 Payment token created successfully!")
    }
}
