package com.paypal.android.corepayments

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ClientIdRepositoryUnitTest {

    private val configuration = CoreConfig("fake-access-token")

    private lateinit var secureTokenServiceAPI: SecureTokenServiceAPI

    private lateinit var sut: ClientIdRepository

    @Before
    fun beforeEach() {
        secureTokenServiceAPI = mockk(relaxed = true)
        ClientIdRepository.clientIDCache.evictAll()
    }

    @Test
    fun `getClientId() on cache miss returns client id from secure token service api`() = runTest {
        coEvery { secureTokenServiceAPI.getClientId() } returns "fake-client-id"
        sut = ClientIdRepository(configuration, secureTokenServiceAPI)

        val result = sut.getClientId()
        assertEquals("fake-client-id", result)
    }

    @Test
    fun `getClientId() on cache hit returns client id from cache`() = runTest {
        ClientIdRepository.clientIDCache.put("fake-access-token", "cached-id-123")
        sut = ClientIdRepository(configuration, secureTokenServiceAPI)

        val result = sut.getClientId()
        assertEquals("cached-id-123", result)
        coVerify(exactly = 0) { secureTokenServiceAPI.getClientId() }
    }
}