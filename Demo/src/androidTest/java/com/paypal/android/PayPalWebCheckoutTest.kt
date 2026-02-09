package com.paypal.android

import android.app.Instrumentation
import android.util.Log
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
import java.io.BufferedReader
import java.io.InputStreamReader

@RunWith(TestParameterInjector::class)
class PayPalWebCheckoutTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { PayPalCheckoutRobot(composeTestRule) }

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val packageName = context.packageName // Gets "com.paypal.android.test" for test APK

        // Extract the actual app package name (remove .test suffix for the main app)
        val appPackageName = packageName.removeSuffix(".test")

        Log.d(TAG, "=== App Link Setup Debug ===")
        Log.d(TAG, "Test package: $packageName")
        Log.d(TAG, "App package: $appPackageName")

        // Check initial app link state
        Log.d(TAG, "Checking initial app link state...")
        val initialState = executeShellCommandWithOutput(
            instrumentation,
            "pm get-app-links $appPackageName"
        )
        Log.d(TAG, "Initial state:\n$initialState")

        // Set app as default handler for all app links (HTTPS URLs with autoVerify)
        // This handles: https://ppcp-mobile-demo-sandbox-87bbd7f0a27f.herokuapp.com
        Log.d(TAG, "Setting app links to STATE_APPROVED for all domains...")
        val setResult = executeShellCommandWithOutput(
            instrumentation,
            "pm set-app-links --package $appPackageName 1 all"
        )
        Log.d(TAG, "Set app-links result: $setResult")

        // Enable user selection for all domains (this is separate from verification state)
        Log.d(TAG, "Enabling user selection state for all domains...")
        val enableResult = executeShellCommandWithOutput(
            instrumentation,
            "pm set-app-links-user-selection --package $appPackageName true all"
        )
        Log.d(TAG, "Enable selection result: $enableResult")

        // Wait for system to process the changes with retry logic
        Log.d(TAG, "Waiting for app link settings to propagate...")
        var verifyState = ""
        var attemptsRemaining = 10
        var isVerified = false

        while (attemptsRemaining > 0 && !isVerified) {
            Thread.sleep(500) // Wait 500ms between checks

            verifyState = executeShellCommandWithOutput(
                instrumentation,
                "pm get-app-links $appPackageName"
            )

            // Check if domain is verified and not in disabled state
            isVerified = verifyState.contains("verified") &&
                    !verifyState.contains("Disabled:")

            if (!isVerified) {
                attemptsRemaining--
                Log.d(TAG, "Not ready yet, retrying... ($attemptsRemaining attempts left)")
            }
        }

        Log.d(TAG, "Final verification state:\n$verifyState")

        if (isVerified) {
            Log.d(TAG, "✓ App links successfully configured and active")
        } else {
            Log.w(TAG, "⚠ App links may not be fully active - proceeding anyway")
        }

        // Check intent filters for custom URL schemes
        Log.d(TAG, "Checking intent filters for custom schemes...")
        val intentFilters = executeShellCommandWithOutput(
            instrumentation,
            "dumpsys package $appPackageName | grep -A 20 'Activity filter'"
        )
        Log.d(TAG, "Intent filters:\n$intentFilters")

        Log.d(TAG, "=== App Link Setup Complete ===")
    }

    private fun executeShellCommandWithOutput(
        instrumentation: Instrumentation,
        command: String
    ): String {
        val output = StringBuilder()
        try {
            val parcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
            val inputStream = java.io.FileInputStream(parcelFileDescriptor.fileDescriptor)
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
            }
            parcelFileDescriptor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: $command", e)
            output.append("ERROR: ${e.message}")
        }
        return output.toString().trim()
    }

    companion object {
        private const val TAG = "PayPalWebCheckoutTest"
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
