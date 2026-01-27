package com.paypal.android.corepayments.browserswitch

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthTabClientUnitTest {

    private lateinit var sut: AuthTabClient
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var options: ChromeCustomTabOptions

    @Before
    fun beforeEach() {
        sut = AuthTabClient()
        activityResultLauncher = mockk(relaxed = true)
        options = ChromeCustomTabOptions(launchUri = "https://example.com/auth".toUri())
    }

    @Test
    fun `launchAuthTab with appLinkUrl launches auth tab with host and path`() {
        val appLinkUrl = "https://example.com/return/path"
        val intentSlot = slot<Intent>()

        sut.launchAuthTab(
            options = options,
            activityResultLauncher = activityResultLauncher,
            appLinkUrl = appLinkUrl,
            returnUrlScheme = null
        )

        verify { activityResultLauncher.launch(capture(intentSlot)) }
        val intent = intentSlot.captured
        assertNotNull(intent)
        assertEquals("https://example.com/auth", intent.data?.toString())
    }

    @Test
    fun `launchAuthTab with returnUrlScheme launches auth tab with scheme`() {
        val returnUrlScheme = "com.example.app"
        val intentSlot = slot<Intent>()

        sut.launchAuthTab(
            options = options,
            activityResultLauncher = activityResultLauncher,
            appLinkUrl = null,
            returnUrlScheme = returnUrlScheme
        )

        verify { activityResultLauncher.launch(capture(intentSlot)) }
        val intent = intentSlot.captured
        assertNotNull(intent)
        assertEquals("https://example.com/auth", intent.data?.toString())
    }

    @Test
    fun `launchAuthTab throws when both appLinkUrl and returnUrlScheme are null`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            sut.launchAuthTab(
                options = options,
                activityResultLauncher = activityResultLauncher,
                appLinkUrl = null,
                returnUrlScheme = null
            )
        }

        assert(exception.message == "Either appLinkUrl or returnUrlScheme must be provided")
    }

    @Test
    fun `launchAuthTab with appLinkUrl with root path launches auth tab successfully`() {
        val appLinkUrl = "https://example.com"
        val intentSlot = slot<Intent>()

        sut.launchAuthTab(
            options = options,
            activityResultLauncher = activityResultLauncher,
            appLinkUrl = appLinkUrl,
            returnUrlScheme = null
        )

        verify { activityResultLauncher.launch(capture(intentSlot)) }
        val intent = intentSlot.captured
        assertNotNull(intent)
        assertEquals("https://example.com/auth", intent.data?.toString())
    }

    @Test
    fun `launchAuthTab prefers appLinkUrl when both appLinkUrl and returnUrlScheme are provided`() {
        val appLinkUrl = "https://example.com/return/path"
        val returnUrlScheme = "com.example.app"
        val intentSlot = slot<Intent>()

        sut.launchAuthTab(
            options = options,
            activityResultLauncher = activityResultLauncher,
            appLinkUrl = appLinkUrl,
            returnUrlScheme = returnUrlScheme
        )

        verify { activityResultLauncher.launch(capture(intentSlot)) }
        val intent = intentSlot.captured
        assertNotNull(intent)
        assertEquals("https://example.com/auth", intent.data?.toString())
    }

    @Test
    fun `launchAuthTab with valid appLinkUrl parses host and path correctly`() {
        val appLinkUrl = "https://paypal.com/checkoutnow/return"
        val intentSlot = slot<Intent>()

        sut.launchAuthTab(
            options = options,
            activityResultLauncher = activityResultLauncher,
            appLinkUrl = appLinkUrl,
            returnUrlScheme = null
        )

        verify { activityResultLauncher.launch(capture(intentSlot)) }
        val intent = intentSlot.captured
        assertNotNull(intent)
        assertEquals("https://example.com/auth", intent.data?.toString())
    }
}
