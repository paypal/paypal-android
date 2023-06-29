package com.paypal.android

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class CardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<DemoActivity>()

    @Test
    fun approveOrder() {
        composeTestRule.onNodeWithText("CARD").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasText("New Visa"))
        composeTestRule.onNodeWithText("New Visa").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasText("Card Details"))

        composeTestRule.onNodeWithText("SCA").performClick()
        composeTestRule.onNodeWithText("WHEN REQUIRED").performClick()

        composeTestRule.onNodeWithText("INTENT").performClick()
        composeTestRule.onNodeWithText("AUTHORIZE").performClick()

        composeTestRule.onNodeWithText("SHOULD VAULT").performClick()
        composeTestRule.onNodeWithText("YES").performClick()

        // insert random customer id for vaulting
        val customerVaultId = UUID.randomUUID().toString()
        composeTestRule.onNodeWithText("CUSTOMER ID FOR VAULT").performTextInput(customerVaultId)

        // press done button and submit form
        composeTestRule.onNodeWithText("CUSTOMER ID FOR VAULT").performImeAction()
        composeTestRule.onNodeWithText("CREATE & APPROVE ORDER").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasText("Status: COMPLETED"), 10_000L)
    }
}