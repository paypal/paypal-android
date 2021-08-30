package com.paypal.android.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

@ExperimentalCoroutinesApi
class HttpUnitTest {

    private val url = mockk<URL>()
    private val urlConnection = mockk<HttpURLConnection>(relaxed = true)
    private val httpRequest = spyk(HttpRequest("https://www.example.com"))

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private lateinit var sut: Http

    @Before
    fun beforeEach() {
        every { httpRequest.url } returns url
        every { url.openConnection() } returns urlConnection

        sut = Http()
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `send sets request method on url connection`() = runBlockingTest {
        sut.send(httpRequest, testCoroutineDispatcher)
        verify { urlConnection.requestMethod = "GET" }
    }

    @Test
    fun `send returns an http result`() = runBlockingTest {
        every { urlConnection.responseCode } returns 123

        val result = sut.send(httpRequest, testCoroutineDispatcher)
        assertEquals(123, result.responseCode)
    }
}
