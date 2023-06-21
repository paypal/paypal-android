package com.paypal.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paypal.android.testutils.AppDriver
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class CardTest {

    companion object {
        val validExpirationYear = Calendar.getInstance().get(Calendar.YEAR) + 3
    }

    private val driver = AppDriver("com.paypal.android")

    @Test
    fun approveOrder() {
        with(driver) {
            launchAppFromHomeScreen()

            findText("CARD").click()
            waitForText("Card Number")

            findText("Card Number").text = "4111111111111111"
            findText("Expiration").text = "01/$validExpirationYear"
            findText("Security Code").text = "123"

            findText("SUBMIT").click()

            waitForText("CAPTURE success: CONFIRMED")
            assertTrue(findText("CAPTURE success: CONFIRMED").exists())
        }
    }
}
