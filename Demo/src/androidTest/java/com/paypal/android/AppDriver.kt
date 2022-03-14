package com.paypal.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
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

    fun findText(text: String): UiObject {
        return device.findObject(UiSelector().text(text))
    }

    fun waitForText(text: String) {
        device.wait(Until.hasObject(By.text(text)), LAUNCH_TIMEOUT)
    }

    private fun getLauncherPackageName(): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        val context: Context = ApplicationProvider.getApplicationContext()
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo!!.activityInfo.packageName
    }
}