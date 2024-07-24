package com.paypal.android.corepayments.features.eligibility

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.OrderIntent
import com.paypal.android.corepayments.apis.eligibility.Eligibility
import com.paypal.android.corepayments.apis.eligibility.EligibilityAPI
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class EligibilityClientUnitTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    // Ref: https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test#dispatchersmain-delegation
    private lateinit var mainThreadSurrogate: ExecutorCoroutineDispatcher

    private lateinit var eligibilityAPI: EligibilityAPI

    @Before
    fun beforeEach() {
        mainThreadSurrogate = newSingleThreadContext("UI thread")
        Dispatchers.setMain(mainThreadSurrogate)

        eligibilityAPI = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun `check() forwards eligibility check success`() = runTest {
        val eligibility = Eligibility(
            isVenmoEligible = true,
            isPayPalEligible = false,
            isPayPalCreditEligible = true,
            isPayLaterEligible = false,
            isCreditCardEligible = true
        )

        val request = EligibilityRequest(OrderIntent.CAPTURE, "USD")
        coEvery {
            eligibilityAPI.checkEligibility(applicationContext, request)
        } returns eligibility

        val listener = mockk<CheckEligibilityResultListener>(relaxed = true)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sut = EligibilityClient(applicationContext, eligibilityAPI, dispatcher)
        sut.check(request, listener)
        advanceUntilIdle()

        val resultSlot = slot<EligibilityResult>()
        verify(exactly = 1) { listener.onCheckEligibilitySuccess(capture(resultSlot)) }

        val actual = resultSlot.captured
        assertTrue(actual.isVenmoEligible)
        assertFalse(actual.isPayPalEligible)
        assertTrue(actual.isCreditEligible)
        assertFalse(actual.isPayLaterEligible)
        assertTrue(actual.isCardEligible)
    }
}