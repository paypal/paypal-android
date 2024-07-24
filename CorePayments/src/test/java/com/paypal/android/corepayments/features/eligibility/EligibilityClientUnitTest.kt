package com.paypal.android.corepayments.features.eligibility

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.OrderIntent
import com.paypal.android.corepayments.PayPalSDKError
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
import org.junit.Assert.assertSame
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
    private val eligibilityRequest = EligibilityRequest(OrderIntent.CAPTURE, "USD")

    private lateinit var eligibilityCheckListener: EligibilityCheckListener

    @Before
    fun beforeEach() {
        mainThreadSurrogate = newSingleThreadContext("UI thread")
        Dispatchers.setMain(mainThreadSurrogate)

        eligibilityAPI = mockk(relaxed = true)
        eligibilityCheckListener = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun `check() forwards eligibility check success to listener`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val eligibility = Eligibility(
            isVenmoEligible = true,
            isPayPalEligible = false,
            isPayPalCreditEligible = true,
            isPayLaterEligible = false,
            isCreditCardEligible = true
        )
        coEvery {
            eligibilityAPI.checkEligibility(applicationContext, eligibilityRequest)
        } returns eligibility

        val sut = EligibilityClient(applicationContext, eligibilityAPI, dispatcher)
        sut.check(eligibilityRequest, eligibilityCheckListener)
        advanceUntilIdle()

        val resultSlot = slot<EligibilityResult>()
        verify(exactly = 1) {
            eligibilityCheckListener.onCheckEligibilitySuccess(capture(resultSlot))
        }

        val actual = resultSlot.captured
        assertTrue(actual.isVenmoEligible)
        assertFalse(actual.isPayPalEligible)
        assertTrue(actual.isCreditEligible)
        assertFalse(actual.isPayLaterEligible)
        assertTrue(actual.isCardEligible)
    }

    @Test
    fun `check() forwards eligibility check failure to listener`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val error = PayPalSDKError(123, "sample error")
        coEvery {
            eligibilityAPI.checkEligibility(applicationContext, eligibilityRequest)
        } throws error

        val sut = EligibilityClient(applicationContext, eligibilityAPI, dispatcher)
        sut.check(eligibilityRequest, eligibilityCheckListener)
        advanceUntilIdle()

        val errorSlot = slot<PayPalSDKError>()
        verify(exactly = 1) {
            eligibilityCheckListener.onCheckEligibilityFailure(capture(errorSlot))
        }
        assertSame(error, errorSlot.captured)
    }
}