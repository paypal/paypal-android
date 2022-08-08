package com.paypal.android.checkout

import android.app.Application
import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.Environment
import com.paypal.android.core.PayPalSDKError
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.Approval
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.error.ErrorInfo
import com.paypal.checkout.error.OnError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.lang.reflect.Field

@ExperimentalCoroutinesApi
class PayPalClientTest {

    private val mockApplication = mockk<Application>(relaxed = true)
    private val mockClientId = generateRandomString()
    private val mockReturnUrl = "com.example://paypalpay"

    private val api = mockk<API>(relaxed = true)

    private lateinit var sut: PayPalClient

    // Ref: https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test#dispatchersmain-delegation
    private lateinit var mainThreadSurrogate: ExecutorCoroutineDispatcher


    @Before
    fun setUp() {
        mockkStatic(PayPalCheckout::class)
        every { PayPalCheckout.setConfig(any()) } just runs
        coEvery { api.getClientId() } returns mockClientId
        mainThreadSurrogate = newSingleThreadContext("UI thread")
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun dispose() {
        unmockkAll()
        resetField(PayPalCheckout::class.java, "isConfigSet", false)
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun `when startCheckout is invoked, PayPalCheckout config is set`() = runTest {
        val configSlot = slot<CheckoutConfig>()
        every { PayPalCheckout.setConfig(capture(configSlot)) } answers { configSlot.captured }

        every {
            PayPalCheckout.startCheckout(any())
        } just runs

        sut = getPayPalCheckoutClient(testScheduler = testScheduler)
        sut.startCheckout(mockk())
        advanceUntilIdle()

        verify {
            PayPalCheckout.setConfig(any())
        }
        expectThat(configSlot.captured) {
            get { clientId }.isEqualTo(mockClientId)
            get { application }.isEqualTo(mockApplication)
            get { environment }.isEqualTo(com.paypal.checkout.config.Environment.SANDBOX)
        }
    }

    @Test
    fun `when startCheckout is invoked, onPayPalCheckoutStart is called`() = runBlocking {
        val payPalCheckoutListener = mockk<PayPalCheckoutListener>(relaxed = true)
        every { PayPalCheckout.startCheckout(any()) } just runs

        sut = getPayPalCheckoutClient()
        sut.listener = payPalCheckoutListener
        resetField(PayPalCheckout::class.java, "isConfigSet", true)
        sut.startCheckout(mockk(relaxed = true))

        verify {
            payPalCheckoutListener.onPayPalCheckoutStart()
        }
    }

    @Test
    fun `when getting client id fails is invoked, it calls onPayPalFailure`() = runTest {
        val mockCode = 0
        val mockErrorDescription = "mock_error_description"
        val error = PayPalSDKError(mockCode, mockErrorDescription)
        val errorSlot = slot<PayPalSDKError>()
        val payPalCheckoutListener = spyk<PayPalCheckoutListener>()

        coEvery { api.getClientId() } throws error
        every {
            payPalCheckoutListener.onPayPalCheckoutFailure(capture(errorSlot))
        } answers { errorSlot.captured }

        sut = getPayPalCheckoutClient(testScheduler = testScheduler)
        sut.listener = payPalCheckoutListener

        sut.startCheckout(mockk(relaxed = true))
        advanceUntilIdle()

        verify {
            payPalCheckoutListener.onPayPalCheckoutFailure(any())
        }
        expectThat(errorSlot.captured) {
            get { code }.isEqualTo(mockCode)
            get { errorDescription }.isEqualTo(mockErrorDescription)
        }
    }

    @Test
    fun `when checkout is invoked with LIVE env, PayPalCheckout config is set with LIVE`() =
        runTest {
            val configSlot = slot<CheckoutConfig>()
            every { PayPalCheckout.setConfig(capture(configSlot)) } answers { configSlot.captured }

            every {
                PayPalCheckout.startCheckout(any())
            } just runs

            sut = getPayPalCheckoutClient(CoreConfig(environment = Environment.LIVE), testScheduler)
            sut.startCheckout(mockk())
            advanceUntilIdle()

            verify {
                PayPalCheckout.setConfig(any())
            }
            expectThat(configSlot.captured) {
                get { clientId }.isEqualTo(mockClientId)
                get { application }.isEqualTo(mockApplication)
                get { environment }.isEqualTo(com.paypal.checkout.config.Environment.LIVE)
            }
        }

    @Test
    fun `when checkout is invoked with STAGING env, PayPalCheckout config is set with STAGE`() =
        runTest {
            val configSlot = slot<CheckoutConfig>()
            every { PayPalCheckout.setConfig(capture(configSlot)) } answers { configSlot.captured }

            every {
                PayPalCheckout.startCheckout(any())
            } just runs

            sut = getPayPalCheckoutClient(CoreConfig(environment = Environment.STAGING), testScheduler)
            sut.startCheckout(mockk())
            advanceUntilIdle()

            verify {
                PayPalCheckout.setConfig(any())
            }
            expectThat(configSlot.captured) {
                get { clientId }.isEqualTo(mockClientId)
                get { application }.isEqualTo(mockApplication)
                get { environment }.isEqualTo(com.paypal.checkout.config.Environment.STAGE)
            }
        }

    @Test
    fun `when startCheckout is invoked, PayPalCheckout startCheckout is called`() = runTest {
        val createOrder = mockk<CreateOrder>(relaxed = true)
        every { PayPalCheckout.startCheckout(any()) } just runs

        sut = getPayPalCheckoutClient(testScheduler = testScheduler)
        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        sut.startCheckout(mockk(relaxed = true))
        advanceUntilIdle()

        verify {
            PayPalCheckout.startCheckout(createOrder)
        }
    }

    @Test
    fun `when listener is set, PayPalCheckout registerCallbacks() is called`() = runTest {
        val listener = mockk<PayPalCheckoutListener>()
        sut = getPayPalCheckoutClient()
        sut.listener = listener

        verify {
            PayPalCheckout.registerCallbacks(
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `when OnApprove is invoked, onPayPalSuccess is called`() {
        val approval = mockk<Approval>()
        val onApproveSlot = slot<OnApprove>()
        val paypalCheckoutResultSlot = slot<PayPalCheckoutResult>()

        every {
            PayPalCheckout.registerCallbacks(
                capture(onApproveSlot),
                any(),
                any(),
                any()
            )
        } answers { onApproveSlot.captured.onApprove(approval) }

        sut = getPayPalCheckoutClient()

        val payPalClientListener = mockk<PayPalCheckoutListener>(relaxed = true)
        sut.listener = payPalClientListener

        every {
            payPalClientListener.onPayPalCheckoutSuccess(capture(paypalCheckoutResultSlot))
        } answers {
            assert(paypalCheckoutResultSlot.captured.approval == approval)
        }

        verify { payPalClientListener.onPayPalCheckoutSuccess(any()) }
    }

    @Test
    fun `when OnCancel is invoked, onPayPalCancel is called`() = runBlocking {
        val onCancelSlot = slot<OnCancel>()

        every {
            PayPalCheckout.registerCallbacks(
                any(),
                any(),
                capture(onCancelSlot),
                any()
            )
        } answers { onCancelSlot.captured.onCancel() }

        sut = getPayPalCheckoutClient()

        val payPalClientListener = mockk<PayPalCheckoutListener>(relaxed = true)
        sut.listener = payPalClientListener

        verify { payPalClientListener.onPayPalCheckoutCanceled() }
    }

    @Test
    fun `when OnError is invoked, onPayPalFailure is called`() {
        val errorMessage = "mock_error_message"
        val onError = slot<OnError>()
        val paypalSdkErrorSlot = slot<PayPalSDKError>()
        val errorInfo = mockk<ErrorInfo>(relaxed = true).also {
            every { it.reason }.returns(errorMessage)
        }

        every {
            PayPalCheckout.registerCallbacks(
                any(),
                any(),
                any(),
                capture(onError)
            )
        } answers { onError.captured.onError(errorInfo) }

        sut = getPayPalCheckoutClient()

        val payPalClientListener = mockk<PayPalCheckoutListener>(relaxed = true)
        sut.listener = payPalClientListener

        every {
            payPalClientListener.onPayPalCheckoutFailure(capture(paypalSdkErrorSlot))
        } answers {
            assert(paypalSdkErrorSlot.captured.errorDescription == errorMessage)
        }

        verify { payPalClientListener.onPayPalCheckoutFailure(any()) }
    }

    /**
     * Resets a private variable on a singleton.
     *
     * @param clazz - Singleton class to modify
     * @param fieldName - Name of the variable to reset
     * @param defaultValue - default value of the variable
     */
    private fun resetField(clazz: Class<*>, fieldName: String, defaultValue: Any?) {
        val instance: Field = clazz.getDeclaredField(fieldName)
        instance.isAccessible = true
        instance.set(null, defaultValue)
    }

    private fun getPayPalCheckoutClient(
        coreConfig: CoreConfig = CoreConfig(),
        testScheduler: TestCoroutineScheduler? = null
    ): PayPalClient {
        return testScheduler?.let {
            val dispatcher = StandardTestDispatcher(testScheduler)
            PayPalClient(mockApplication, coreConfig, mockReturnUrl, api, dispatcher)
        }?: PayPalClient(mockApplication, coreConfig, mockReturnUrl, api)
    }
}
