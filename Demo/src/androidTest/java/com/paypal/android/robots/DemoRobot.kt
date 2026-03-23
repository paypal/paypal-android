package com.paypal.android.robots

import android.util.Log
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
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
import com.paypal.android.uishared.enums.ReturnToAppStrategyOption
import com.paypal.android.uishared.enums.StoreInVaultOption
import com.paypal.android.utils.TestConstants.TIMEOUT_LONG_MS

/**
 * Provides API for testing PayPal checkout workflows
 */
@OptIn(ExperimentalTestApi::class)
class DemoRobot(
    private val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
) {

    private val otpPageRobot: OtpPageRobot by lazy { OtpPageRobot() }
    private val chromeRobot: ChromeRobot by lazy { ChromeRobot() }
    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }
    private val webPageRobot = PayPalWebPageRobot()

    companion object {
        private const val TAG = "DemoRobot"
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
        composeTestRule.waitUntilExactlyOneExists(
            hasText("ORDER CREATED"),
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

    fun setStoreInVaultOption(storeInVaultOption: StoreInVaultOption) = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("STORE IN VAULT"),
            TIMEOUT_LONG_MS
        )
        val optionText = when (storeInVaultOption) {
            StoreInVaultOption.ON_SUCCESS -> "ON_SUCCESS"
            StoreInVaultOption.NO -> "NO"
        }
        composeTestRule.onNodeWithText(optionText).performClick()
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
        Log.d(TAG, "🚪 Starting PayPal checkout with login for email: $email")
        webPageRobot.checkout(email, password)

        // Wait for return to app and checkout completion
        waitForAppToReturn()

        composeTestRule.waitUntilExactlyOneExists(hasText("CHECKOUT COMPLETE"), TIMEOUT_LONG_MS)

        // Verify Order ID and Payer ID labels are present
        composeTestRule.waitUntilExactlyOneExists(hasText("Order ID"))
        composeTestRule.waitUntilExactlyOneExists(hasText("Payer ID"))

        Log.d(TAG, "🚀 PayPal checkout with login completed successfully")
    }

    fun completeOrder(intent: String? = null) = apply {
        val completeButtonText = when (intent) {
            "AUTHORIZE" -> "AUTHORIZE ORDER"
            "CAPTURE" -> "CAPTURE ORDER"
            else -> "COMPLETE ORDER"
        }
        // Wait for Step 3 to appear
        composeTestRule.waitUntilExactlyOneExists(hasText("Complete Order"), TIMEOUT_LONG_MS)

        // Click on "COMPLETE ORDER" button
        composeTestRule.waitUntilExactlyOneExists(hasText(completeButtonText))
        composeTestRule.onNodeWithText(completeButtonText).performClick()

        val expectedFinalStatus = when (intent) {
            "AUTHORIZE" -> "ORDER AUTHORIZED"
            "CAPTURE" -> "ORDER CAPTURED"
            else -> "ORDER COMPLETED"
        }

        // Wait for order completion and verify success
        composeTestRule.waitUntilExactlyOneExists(hasText(expectedFinalStatus), TIMEOUT_LONG_MS)

        Log.d(TAG, "🎉 Order completed successfully with status: $expectedFinalStatus")
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

    fun navigateToCardVault() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Vault")
        )
        composeTestRule.onNodeWithText("Vault").performClick()
        composeTestRule.onNodeWithText("Vault Card").isDisplayed()
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Create Setup Token")
        )
    }

    fun createSetupToken(
        returnToAppStrategy: ReturnToAppStrategyOption,
        sca: com.paypal.android.cardpayments.threedsecure.SCA
    ) = apply {
        setSCA(sca)
        setReturnToAppStrategyOption(returnToAppStrategy)
        clickCreateSetupToken()
        verifySetupTokenCreated()

        Log.d(
            TAG,
            "✅ Setup token created successfully with SCA: $sca, " +
                    "returnToAppStrategy: $returnToAppStrategy"
        )
    }

    fun setSCA(sca: com.paypal.android.cardpayments.threedsecure.SCA) = apply {
        composeTestRule.waitUntilExactlyOneExists(hasText("SCA"), TIMEOUT_LONG_MS)
        val scaText = when (sca) {
            com.paypal.android.cardpayments.threedsecure.SCA.SCA_ALWAYS -> "SCA_ALWAYS"
            com.paypal.android.cardpayments.threedsecure.SCA.SCA_WHEN_REQUIRED -> "SCA_WHEN_REQUIRED"
        }
        composeTestRule.onNodeWithText(scaText).performClick()
    }

    fun pickTestCard(cardName: String) = apply {
        composeTestRule.onNodeWithText("Use a Test Card", true).isDisplayed()
        composeTestRule.onNodeWithText("Use a Test Card", true).performClick()

        composeTestRule.waitUntilExactlyOneExists(
            hasText(cardName),
            TIMEOUT_LONG_MS
        )
        composeTestRule.onNodeWithText(cardName).performClick()

        Log.d(TAG, "✅ Selected test card: $cardName")
    }

    fun vaultCard() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("VAULT CARD"),
            TIMEOUT_LONG_MS
        )
        composeTestRule.onNodeWithText("VAULT CARD").performClick()
    }

    fun verify3DSChallenge(otpCode: String = "1234") = apply {
        val chromeOpened = chromeRobot.waitForChromeAppToAppear()
        if (!chromeOpened) {
            Log.d(TAG, "Chrome did not open for 3DS challenge")
        } else {
            Log.d(TAG, "Chrome opened for 3DS challenge, proceeding with OTP verification")
            otpPageRobot.verifyOtpPage(otpCode)
        }
        waitForAppToReturn()
    }

    fun verifyCardVaulted() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("CARD VAULTED"),
            TIMEOUT_LONG_MS
        )
        composeTestRule.onNodeWithText("Setup Token ID").isDisplayed()

        Log.d(TAG, "🎉 Card vaulted successfully!")
    }

    fun createOrder(
        intent: String,
        returnToAppStrategy: ReturnToAppStrategyOption,
    ) = apply {
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

    fun clickApproveOrder() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("APPROVE ORDER"),
            TIMEOUT_LONG_MS
        )
        composeTestRule.onNodeWithText("APPROVE ORDER").performClick()
    }

    fun navigateToApproveOrder() = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Approve Order")
        )
        composeTestRule.onNodeWithText("Approve Order").performClick()
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Card Approve Order"),
            TIMEOUT_LONG_MS
        )
    }
}
