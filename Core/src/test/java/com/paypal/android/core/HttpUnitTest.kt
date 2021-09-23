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
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

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
    fun `send sets request method on url connection`() = runBlockingTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)

        sut.send(httpRequest)
        verify { urlConnection.requestMethod = "GET" }
    }

    @Test
    fun `send sets request headers on url connection`() = runBlockingTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)
        httpRequest.headers["Sample-Header"] = "sample-value"

        sut.send(httpRequest)
        verify { urlConnection.addRequestProperty("Sample-Header", "sample-value") }
    }

    @Test
    fun `send calls connect on http url connection to initiate request`() = runBlockingTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)

        sut.send(httpRequest)
        verify { urlConnection.connect() }
    }

    @Test
    fun `send forwards http response from http parser`() = runBlockingTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)
        val result = sut.send(httpRequest)

        assertSame(httpResponse, result)
    }
}
