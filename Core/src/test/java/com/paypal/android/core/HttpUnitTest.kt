package com.paypal.android.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class HttpUnitTest {

    @ExperimentalCoroutinesApi
    val testCoroutineDispatcher = TestCoroutineDispatcher()

    @ExperimentalCoroutinesApi
    @Test
    fun `send sets request method on url connection`() = runBlockingTest {
        val httpRequest = spyk(HttpRequest("https://www.example.com"))

        val url = mockk<URL>()
        every { httpRequest.url } returns url

        val urlConnection = mockk<HttpURLConnection>(relaxed = true)
        every { url.openConnection() } returns urlConnection

        val sut = Http()
        sut.send(httpRequest, testCoroutineDispatcher)

        verify { urlConnection.requestMethod = "GET" }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `send returns an http result`() = runBlockingTest {
        val httpRequest = spyk(HttpRequest("https://www.example.com"))

        val url = mockk<URL>()
        every { httpRequest.url } returns url

        val urlConnection = mockk<HttpURLConnection>(relaxed = true)
        every { url.openConnection() } returns urlConnection
        every { urlConnection.responseCode } returns 123

        val sut = Http()
        val result = sut.send(httpRequest, testCoroutineDispatcher)

        assertEquals(123, result.responseCode)
    }
}
