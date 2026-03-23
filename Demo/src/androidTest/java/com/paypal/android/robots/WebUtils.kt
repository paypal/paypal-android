package com.paypal.android.robots

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.paypal.android.utils.TestConstants.TIMEOUT_SHORT_MS

private const val TAG = "WebUtils"
val device: UiDevice by lazy {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
}

fun findElement(selectors: List<BySelector>): UiObject2? {
    for (selector in selectors) {
        val element = device.wait(Until.findObject(selector), TIMEOUT_SHORT_MS)
        if (element != null) {
            return element
        }
    }
    return null
}

fun UiObject2.clearAndEnterText(text: String): Boolean = try {
    click()
    device.waitForIdle()
    clear()
    device.waitForIdle()
    setText(text)
    device.waitForIdle()
    true
} catch (e: Exception) {
    Log.e(TAG, "Failed to enter text '$text'", e)
    false
}

fun logPageHierarchy() {
    try {
        Log.d(TAG, "📋 Current visible elements on page:")
        val allElements = device.findObjects(By.clazz("android.view.View"))
        allElements.take(20).forEachIndexed { index, element ->
            val text = element.text ?: ""
            val contentDesc = element.contentDescription ?: ""
            if (text.isNotEmpty() || contentDesc.isNotEmpty()) {
                Log.d(TAG, "  [$index] Text: '$text' | ContentDesc: '$contentDesc'")
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "⚠️ Failed to log page hierarchy: ${e.message}")
    }
}
