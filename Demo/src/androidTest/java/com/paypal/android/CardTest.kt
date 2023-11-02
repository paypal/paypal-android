package com.paypal.android

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class CardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun approveOrder() {
        val waitTimeoutMs = 15_000L

        composeTestRule.onNodeWithText("Approve Order").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasText("New Visa"), waitTimeoutMs)
        composeTestRule.onNodeWithText("New Visa").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasText("Create Order"), waitTimeoutMs)
        composeTestRule.onNodeWithText("Create Order").performClick()

        // wait for approve order form
        composeTestRule.waitUntilExactlyOneExists(hasText("APPROVE ORDER"), waitTimeoutMs)

        // skip 3DS for this test
        composeTestRule.onNodeWithText("SCA").performClick()
        composeTestRule.onNodeWithText("WHEN REQUIRED").performClick()

        // approve the order
        composeTestRule.onNodeWithText("APPROVE ORDER").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasText("Complete Order"), waitTimeoutMs)
        composeTestRule.onNodeWithText("AUTHORIZE ORDER").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasText("Order Complete"), waitTimeoutMs)
    }
}
