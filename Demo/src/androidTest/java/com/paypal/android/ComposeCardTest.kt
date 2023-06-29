package com.paypal.android

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ComposeCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<DemoActivity>()

    @Test
    fun approveOrder() {
        composeTestRule.onNodeWithText("CARD").performClick()
    }
}