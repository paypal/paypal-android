package com.paypal.android.core

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.net.URL

@ExperimentalCoroutinesApi
class APIUnitTest {

    companion object {
        private const val CLIENT_ID = "sample-client-id"
        private const val CLIENT_SECRET = "sample-client-secret"
    }

    private val http = mockk<Http>()
    private val httpRequestFactory = mockk<HttpRequestFactory>()

    private val apiRequest = APIRequest("/sample/path", HttpMethod.GET, null)
    private val configuration = CoreConfig(CLIENT_ID, CLIENT_SECRET)

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private lateinit var sut: API

    @Before
    fun beforeEach() {
        sut = API(configuration, http, httpRequestFactory)

        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `converts an api request to an http request and sends it`() = runBlocking {
        val url = URL("https://example.com/resolved/path")

        val httpRequest = HttpRequest(url, HttpMethod.GET)
        every {
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        } returns httpRequest

        val httpResponse = HttpResponse(200)
        coEvery {
            http.send(httpRequest)
        } returns httpResponse

        val result = sut.send(apiRequest)
        assertSame(httpResponse, result)
    }
}
