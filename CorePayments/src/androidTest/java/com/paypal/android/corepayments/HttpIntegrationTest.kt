package com.paypal.android.corepayments

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class HttpIntegrationTest {

    @Test
    fun send_makesAnHttpRequest() = runTest {
        val request = HttpRequest(URL("https://www.google.com"))

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val sut = Http(testDispatcher)
        val result = sut.send(request)
        assertEquals(200, result.status)
    }
}
