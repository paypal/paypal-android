package com.paypal.android.corepayments

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class HttpUnitTest {

    private val url = spyk(URL("https://www.example.com"))
    private val urlConnection = mockk<HttpURLConnection>(relaxed = true)

    private val httpResponse = HttpResponse(123)
    private val httpResponseParser = mockk<HttpResponseParser>()

    // Ref: https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test#dispatchersmain-delegation
    private lateinit var mainThreadSurrogate: ExecutorCoroutineDispatcher

    @Before
    fun beforeEach() {
        mainThreadSurrogate = newSingleThreadContext("UI thread")
        Dispatchers.setMain(mainThreadSurrogate)

        every { url.openConnection() } returns urlConnection
        every { httpResponseParser.parse(urlConnection) } returns httpResponse
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun `send sets request method on url connection`() = runTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)

        val sut = createHttp(testScheduler)
        sut.send(httpRequest)
        verify { urlConnection.requestMethod = "GET" }
    }

    @Test
    fun `send sets request headers on url connection`() = runTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)
        httpRequest.headers["Sample-Header"] = "sample-value"

        val sut = createHttp(testScheduler)
        sut.send(httpRequest)
        verify { urlConnection.addRequestProperty("Sample-Header", "sample-value") }
    }

    @Test
    fun `send calls connect on http url connection to initiate request`() = runTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)

        val sut = createHttp(testScheduler)
        sut.send(httpRequest)
        verify { urlConnection.connect() }
    }

    @Test
    fun `send forwards http response from http parser`() = runTest {
        val httpRequest = HttpRequest(url, HttpMethod.GET)

        val sut = createHttp(testScheduler)
        val result = sut.send(httpRequest)

        assertSame(httpResponse, result)
    }

    @Test
    fun `it returns unknown host http status when UnknownHostException thrown`() = runTest {
        val error = UnknownHostException()
        every { urlConnection.connect() } throws error

        val httpRequest = HttpRequest(url, HttpMethod.GET)
        val sut = createHttp(testScheduler)
        val result = sut.send(httpRequest)

        assertEquals(HttpResponse.STATUS_UNKNOWN_HOST, result.status)
        assertSame(error, result.error)
    }

    @Test
    fun `it returns status undetermined when the status cannot be determined`() = runTest {
        val error = Exception()
        every { urlConnection.connect() } throws error

        val httpRequest = HttpRequest(url, HttpMethod.GET)
        val sut = createHttp(testScheduler)
        val result = sut.send(httpRequest)

        assertEquals(HttpResponse.STATUS_UNDETERMINED, result.status)
        assertSame(error, result.error)
    }

    private fun createHttp(testScheduler: TestCoroutineScheduler): Http {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return Http(dispatcher, httpResponseParser)
    }
}
