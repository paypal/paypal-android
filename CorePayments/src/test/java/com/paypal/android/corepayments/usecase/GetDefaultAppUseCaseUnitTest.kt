package com.paypal.android.corepayments.usecase

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetDefaultAppUseCaseUnitTest {

    private val packageManager: PackageManager = mockk()
    private val uri: Uri = Uri.parse("https://example.com")
    private lateinit var sut: GetDefaultAppUseCase

    @Before
    fun beforeEach() {
        sut = GetDefaultAppUseCase(packageManager)
    }

    @Test
    fun `returns the resolved default handler package name`() {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply { packageName = "com.android.chrome" }
        }
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo

        assertEquals("com.android.chrome", sut(uri))
    }

    @Test
    fun `queries resolveActivity with ACTION_VIEW, the given uri, and BROWSABLE category`() {
        val intentSlot = slot<Intent>()
        every {
            packageManager.resolveActivity(capture(intentSlot), PackageManager.MATCH_DEFAULT_ONLY)
        } returns null

        sut(uri)

        val captured = intentSlot.captured
        assertEquals(Intent.ACTION_VIEW, captured.action)
        assertEquals(uri, captured.data)
        assertTrue(captured.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun `returns null when no activity resolves the uri`() {
        every { packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) } returns null

        assertNull(sut(uri))
    }

    @Test
    fun `returns null when resolveInfo has no activityInfo`() {
        val resolveInfo = ResolveInfo().apply { activityInfo = null }
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo

        assertNull(sut(uri))
    }
}
