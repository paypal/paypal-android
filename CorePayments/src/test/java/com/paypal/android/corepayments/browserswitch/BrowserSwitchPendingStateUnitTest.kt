package com.paypal.android.corepayments.browserswitch

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
class BrowserSwitchPendingStateUnitTest {

    @Test
    fun `fromBase64 rejects a non-string target uri`() {
        val invalidTargetUris = listOf(JSONObject(), JSONArray(), JSONObject.NULL)

        invalidTargetUris.forEach { targetUri ->
            val json = JSONObject()
                .put(BrowserSwitchPendingState.KEY_TARGET_URI, targetUri)
                .put(BrowserSwitchPendingState.KEY_REQUEST_CODE, 123)
            val data = json.toString().toByteArray(StandardCharsets.UTF_8)
            val encodedState = Base64.encodeToString(data, Base64.DEFAULT or Base64.NO_WRAP)

            assertNull(BrowserSwitchPendingState.fromBase64(encodedState))
        }
    }
}
