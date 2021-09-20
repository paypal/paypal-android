package com.paypal.android.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class HttpIntegrationTest {

    private lateinit var testDispatcher: TestCoroutineDispatcher

    @Before
    fun beforeEach() {
        testDispatcher = TestCoroutineDispatcher()
    }

    @After
    fun afterEach() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun send_makesAnHttpRequest() = runBlockingTest {
        val request = HttpRequest("https://www.google.com")

        val sut = Http(testDispatcher)
        val result = sut.send(request)
        assertEquals(200, result.status)
    }
}
