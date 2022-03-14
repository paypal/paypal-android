package com.paypal.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.*
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardTest {

    private val driver = AppDriver("com.paypal.android")

    @Test
    fun approveOrder() {
        with(driver) {

            launchAppFromHomeScreen()

            findText("CARD").click()
            waitForText("Card Number")

            findText("Card Number").text = "4111111111111111"
            findText("Expiration").text = "02/2023"
            findText("Security Code").text = "123"

            findText("SUBMIT").click()

            waitForText("CAPTURE success: CONFIRMED")
            assertTrue(findText("CAPTURE success: CONFIRMED").exists())
        }
    }
}
