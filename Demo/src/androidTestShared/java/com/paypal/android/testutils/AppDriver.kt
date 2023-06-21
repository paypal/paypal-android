package com.paypal.android.testutils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert

// Ref: https://github.com/android/testing-samples
class AppDriver(private val appPackage: String) {

    companion object {
        const val LAUNCH_TIMEOUT = 10000L
    }

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun launchAppFromHomeScreen() {
        device.pressHome()

        val launcherPackage = getLauncherPackageName()
        Assert.assertNotNull(launcherPackage)
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT)

        val context: Context = ApplicationProvider.getApplicationContext()
        val intent = context.packageManager.getLaunchIntentForPackage(appPackage)!!
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(appPackage).depth(0)), LAUNCH_TIMEOUT)
    }

    fun findResById(id: String): UiObject {
        return device.findObject(UiSelector().resourceId(id))
    }

    fun findText(text: String): UiObject {
        return device.findObject(UiSelector().text(text))
    }

    fun waitForText(text: String, timeout: Long? = null) {
        device.wait(Until.hasObject(By.text(text)), timeout ?: LAUNCH_TIMEOUT)
    }

    private fun getLauncherPackageName(): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        val context: Context = ApplicationProvider.getApplicationContext()
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo!!.activityInfo.packageName
    }
}
