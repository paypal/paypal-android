package com.paypal.android.fraudprotection

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Test
import org.junit.Assert.assertEquals
import java.util.UUID

class UUIDHelperUnitTest {
    @Test
    fun `when appGUID is not set, uuidHelper returns a new one`() {
        val mockUUID = "mock-uuid"
        mockkObject(SharedPreferenceUtils)
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns mockUUID
        every { SharedPreferenceUtils.instance.getString(any(), any(), any()) } returns null
        every { SharedPreferenceUtils.instance.putString(any(), any(), any()) } just runs

        val sut = UUIDHelper()
        val appGuid = sut.getInstallationGUID(mockk(relaxed = true))
        assertEquals(appGuid, mockUUID)
    }

    @Test
    fun `when appGUID has been set, uuidHelper returns it`() {
        val mockUUID = "mock-uuid"
        mockkObject(SharedPreferenceUtils)
        every { SharedPreferenceUtils.instance.getString(any(), any(), any()) } returns mockUUID

        val sut = UUIDHelper()
        val appGuid = sut.getInstallationGUID(mockk(relaxed = true))
        assertEquals(appGuid, mockUUID)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }
}
