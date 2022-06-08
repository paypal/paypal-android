package com.paypal.android.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

@ExperimentalCoroutinesApi
class HttpUnitTest {

    private val url = spyk(URL("https://www.example.com"))
    private val urlConnection = mockk<HttpURLConnection>(relaxed = true)

    private val httpResponse = HttpResponse(123)
    private val httpResponseParser = mockk<HttpResponseParser>()

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private lateinit var sut: Http

    @Before
    fun beforeEach() {
        every { url.openConnection() } returns urlConnection
        every { httpResponseParser.parse(urlConnection) } returns httpResponse

        sut = Http(testCoroutineDispatcher, httpResponseParser)
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `send sets request method on url connection`() = runTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)

        sut.send(httpRequest)
        verify { urlConnection.requestMethod = "GET" }
    }

    @Test
    fun `send sets request headers on url connection`() = runTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)
        httpRequest.headers["Sample-Header"] = "sample-value"

        sut.send(httpRequest)
        verify { urlConnection.addRequestProperty("Sample-Header", "sample-value") }
    }

    @Test
    fun `send calls connect on http url connection to initiate request`() = runTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)

        sut.send(httpRequest)
        verify { urlConnection.connect() }
    }

    @Test
    fun `send forwards http response from http parser`() = runTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)
        val result = sut.send(httpRequest)

        assertSame(httpResponse, result)
    }

    @Test
    fun `it returns unknown host http status when UnknownHostException thrown`() = runTest {
        val error = UnknownHostException()
        every { urlConnection.connect() } throws error

        val httpRequest = HttpRequest(url, HttpMethod.GET)
        val result = sut.send(httpRequest)

        assertEquals(HttpResponse.STATUS_UNKNOWN_HOST, result.status)
        assertSame(error, result.error)
    }

    @Test
    fun `it returns status undetermined when the status cannot be determined`() = runTest {
        val error = Exception()
        every { urlConnection.connect() } throws error

        val httpRequest = HttpRequest(url, HttpMethod.GET)
        val result = sut.send(httpRequest)

        assertEquals(HttpResponse.STATUS_UNDETERMINED, result.status)
        assertSame(error, result.error)
    }
}
