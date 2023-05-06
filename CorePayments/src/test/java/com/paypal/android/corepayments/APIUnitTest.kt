package com.paypal.android.corepayments

import com.paypal.android.corepayments.analytics.AnalyticsService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class APIUnitTest {

    private val http = mockk<Http>(relaxed = true)
    private val httpRequestFactory = mockk<HttpRequestFactory>()
    private val analyticsService = mockk<AnalyticsService>()

    private val apiRequest = APIRequest("/sample/path", HttpMethod.GET, null)
    private val configuration = CoreConfig("fake-access-token")

    private val httpResponseHeaders = mapOf(
        "Paypal-Debug-Id" to "sample-correlation-id"
    )

    private val url = URL("https://example.com/resolved/path")
    private val httpRequest = HttpRequest(url, HttpMethod.GET)

    private val clientIdSuccessResponse by lazy {
        val clientIdBody = JSONObject()
            .put("client_id", "sample-client-id")
            .toString()
        HttpResponse(200, httpResponseHeaders, clientIdBody)
    }

    private lateinit var sut: API

    @Before
    fun beforeEach() {
        sut = API(configuration, http, httpRequestFactory)
    }

    @Test
    fun `send() converts an api request to an http request and sends it`() = runTest {
        every {
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        } returns httpRequest

        val httpResponse = HttpResponse(200)
        coEvery { http.send(httpRequest) } returns httpResponse

        val result = sut.send(apiRequest)
        assertSame(httpResponse, result)
    }
}
