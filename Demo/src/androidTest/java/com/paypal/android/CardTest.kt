package com.paypal.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paypal.android.testutils.AppDriver
import org.junit.Assert.assertEquals
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
            findText("New Visa").click()

            waitForText("CARD NUMBER")

            findText("SCA").click()
            findText("WHEN REQUIRED").click()

            findText("INTENT").click()
            findText("CAPTURE").click()

            findText("SHOULD VAULT").click()
            findText("NO").click()

            findText("CREATE & APPROVE ORDER").click()

            waitForText("Status: COMPLETED")
            val statusText = findResById("statusText")
            assertEquals("Status: COMPLETED", statusText.text)
        }
    }
}
