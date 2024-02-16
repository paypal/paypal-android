package com.paypal.android.corepayments

import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class RestClientUnitTest {

    private val apiGETRequest = APIRequest("sample/path", HttpMethod.GET, null)

    private val requestBody = """{ "sample": "json" }"""
    private val apiPOSTRequest = APIRequest("sample/path", HttpMethod.POST, requestBody)

    private val httpSuccessResponse = HttpResponse(200)

    private val sandboxConfig = CoreConfig("fake-sandbox-client-id", Environment.SANDBOX)
    private val liveConfig = CoreConfig("fake-live-client-id", Environment.LIVE)

    private lateinit var http: Http
    private lateinit var httpRequestSlot: CapturingSlot<HttpRequest>

    private lateinit var sut: RestClient

    @Before
    fun beforeEach() {
        http = mockk(relaxed = true)
        httpRequestSlot = slot()
    }

    @Test
    fun `send() should properly format the url for the sandbox environment`() = runTest {
        coEvery { http.send(capture(httpRequestSlot)) } returns httpSuccessResponse

        sut = RestClient(sandboxConfig, http, "en_US")
        sut.send(apiGETRequest)

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://api-m.sandbox.paypal.com/sample/path"), httpRequest.url)
    }

    @Test
    fun `send() should properly format the url for the live environment`() = runTest {
        coEvery { http.send(capture(httpRequestSlot)) } returns httpSuccessResponse

        sut = RestClient(liveConfig, http, "en_US")
        sut.send(apiGETRequest)

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://api-m.paypal.com/sample/path"), httpRequest.url)
    }

    @Test
    fun `send() should forward the http method`() = runTest {
        coEvery { http.send(capture(httpRequestSlot)) } returns httpSuccessResponse

        sut = RestClient(liveConfig, http, "en_US")
        sut.send(apiPOSTRequest)

        val httpRequest = httpRequestSlot.captured
        assertEquals(HttpMethod.POST, httpRequest.method)
    }

    @Test
    fun `send() should forward the http body`() = runTest {
        coEvery { http.send(capture(httpRequestSlot)) } returns httpSuccessResponse

        sut = RestClient(liveConfig, http, "en_US")
        sut.send(apiPOSTRequest)

        val httpRequest = httpRequestSlot.captured
        assertEquals(requestBody, httpRequest.body)
    }

    @Test
    fun `send() should set accept encoding default header`() = runTest {
        coEvery { http.send(capture(httpRequestSlot)) } returns httpSuccessResponse

        sut = RestClient(liveConfig, http, "en_US")
        sut.send(apiPOSTRequest)

        val httpRequest = httpRequestSlot.captured
        assertEquals("gzip", httpRequest.headers["Accept-Encoding"])
    }

    @Test
    fun `send() should set accept language default header`() = runTest {
        coEvery { http.send(capture(httpRequestSlot)) } returns httpSuccessResponse

        sut = RestClient(liveConfig, http, "en_US")
        sut.send(apiPOSTRequest)

        val httpRequest = httpRequestSlot.captured
        assertEquals("en_US", httpRequest.headers["Accept-Language"])
    }

    @Test
    fun `send() should add basic authorization header using client id from config`() = runTest {
        coEvery { http.send(capture(httpRequestSlot)) } returns httpSuccessResponse

        sut = RestClient(liveConfig, http, "en_US")
        sut.send(apiPOSTRequest)

        val httpRequest = httpRequestSlot.captured
        assertEquals("Basic ZmFrZS1saXZlLWNsaWVudC1pZDo=", httpRequest.headers["Authorization"])
    }

    @Test
    fun `send() should add content type json header for POST requests`() = runTest {
        coEvery { http.send(capture(httpRequestSlot)) } returns httpSuccessResponse

        sut = RestClient(liveConfig, http, "en_US")
        sut.send(apiPOSTRequest)

        val httpRequest = httpRequestSlot.captured
        assertEquals("application/json", httpRequest.headers["Content-Type"])
    }

    @Test
    fun `send() should add content type json header for GET requests`() = runTest {
        coEvery { http.send(capture(httpRequestSlot)) } returns httpSuccessResponse

        sut = RestClient(liveConfig, http, "en_US")
        sut.send(apiGETRequest)

        val httpRequest = httpRequestSlot.captured
        assertNull(httpRequest.headers["Content-Type"])
    }

    @Test
    fun `send() should send http request and forward result to caller`() = runTest {
        coEvery { http.send(capture(httpRequestSlot)) } returns httpSuccessResponse

        sut = RestClient(liveConfig, http, "en_US")
        val result = sut.send(apiGETRequest)

        assertSame(httpSuccessResponse, result)
    }
}
