package com.paypal.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.*
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardTest {

    companion object {
        const val LAUNCH_TIMEOUT = 5000L
        const val APP_PACKAGE = "com.paypal.android"
    }

    private lateinit var device: UiDevice

    @Test
    fun useAppContext() {
        // Ref: https://github.com/android/testing-samples
        device = UiDevice.getInstance(getInstrumentation())
        device.pressHome()

        val launcherPackage = getLauncherPackageName()
        assertNotNull(launcherPackage)
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT)

        val context: Context = getApplicationContext()
        val intent = context.packageManager.getLaunchIntentForPackage(APP_PACKAGE)!!
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_TIMEOUT)

        waitForText("CARD")
        findText("CARD").click()

        findText("Card Number").text = "4111111111111111"
        findText("Expiration").text = "0223"
        findText("Security Code").text = "123"
    }

    private fun findText(buttonText: String): UiObject {
        return device.findObject(UiSelector().text(buttonText))
    }

    private fun waitForText(buttonText: String) {
        device.wait(Until.hasObject(By.text(buttonText)), LAUNCH_TIMEOUT)
    }

    private fun getLauncherPackageName(): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        val context: Context = getApplicationContext()
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo!!.activityInfo.packageName
    }
}
