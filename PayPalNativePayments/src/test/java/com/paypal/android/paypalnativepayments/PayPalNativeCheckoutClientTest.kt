package com.paypal.android.paypalnativepayments

import android.app.Application
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.Approval
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.error.ErrorInfo
import com.paypal.checkout.error.OnError
import com.paypal.checkout.shipping.OnShippingChange
import com.paypal.checkout.shipping.ShippingChangeActions
import com.paypal.checkout.shipping.ShippingChangeData
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.lang.reflect.Field

@ExperimentalCoroutinesApi
class PayPalNativeCheckoutClientTest {

    private val mockApplication = mockk<Application>(relaxed = true)
    private val mockClientId = generateRandomString()
    private val mockReturnUrl = "mock_return_url"

    private val api = mockk<API>(relaxed = true)

    private lateinit var sut: PayPalNativeCheckoutClient

    @Before
    fun setUp() {
        mockkStatic(PayPalCheckout::class)
        every { PayPalCheckout.setConfig(any()) } just runs
        coEvery { api.fetchCachedOrRemoteClientID() } returns mockClientId
    }

    @After
    fun dispose() {
        unmockkAll()
        resetField(PayPalCheckout::class.java, "isConfigSet", false)
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
    fun `when startCheckout is invoked with an invalid return_url, onPayPalCheckout failure is called`() = runTest {
        every { PayPalCheckout.setConfig(any()) } throws IllegalArgumentException(CheckoutConfig.INVALID_RETURN_URL)
        val payPalCheckoutListener = spyk<PayPalNativeCheckoutListener>()
        val errorSlot = slot<PayPalSDKError>()
        every {
            payPalCheckoutListener.onPayPalCheckoutFailure(capture(errorSlot))
        } answers { errorSlot.captured }

        every {
            PayPalCheckout.startCheckout(any())
        } just runs

        sut = getPayPalCheckoutClient(testScheduler = testScheduler)
        sut.listener = payPalCheckoutListener

        sut.startCheckout(mockk())
        advanceUntilIdle()

        verify {
            payPalCheckoutListener.onPayPalCheckoutFailure(any())
        }

        expectThat(errorSlot.captured) {
            get { code }.isEqualTo(0)
            get { errorDescription }.isEqualTo(CheckoutConfig.INVALID_RETURN_URL)
        }
    }

    @Test
    fun `when startCheckout is invoked, onPayPalCheckoutStart is called`() = runTest {
        val payPalCheckoutListener = mockk<PayPalNativeCheckoutListener>(relaxed = true)
        every { PayPalCheckout.startCheckout(any()) } just runs

        sut = getPayPalCheckoutClient(testScheduler = testScheduler)
        sut.listener = payPalCheckoutListener
        resetField(PayPalCheckout::class.java, "isConfigSet", true)
        sut.startCheckout(mockk(relaxed = true))
        advanceUntilIdle()

        verify {
            payPalCheckoutListener.onPayPalCheckoutStart()
        }
    }

    @Test
    fun `when getting client id fails is invoked, it calls onPayPalFailure`() = runTest {
        val error = PayPalSDKError(123, "fake-description")
        val errorSlot = slot<PayPalSDKError>()
        val payPalCheckoutListener = spyk<PayPalNativeCheckoutListener>()

        coEvery { api.fetchCachedOrRemoteClientID() } throws error
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
            get { code }.isEqualTo(123)
            get { errorDescription }.isEqualTo("Error fetching clientID. Contact developer.paypal.com/support.")
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

            val config = CoreConfig("fake-access-token", Environment.LIVE)
            sut = getPayPalCheckoutClient(config, testScheduler)
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

            val config = CoreConfig("fake-access-token", Environment.STAGING)
            sut = getPayPalCheckoutClient(config, testScheduler)
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
        val request = PayPalNativeCheckoutRequest("mock_order_id")
        every { PayPalCheckout.startCheckout(any()) } just runs

        sut = getPayPalCheckoutClient(testScheduler = testScheduler)
        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        sut.startCheckout(request)
        advanceUntilIdle()

        verify {
            PayPalCheckout.startCheckout(any())
        }
    }

    @Test
    fun `when listener is set, PayPalCheckout registerCallbacks() is called`() = runTest {
        val listener = mockk<PayPalNativeCheckoutListener>()
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
        val mockOrderID = "mock_order_id"
        val mockPayerID = "mock_payer_id"
        val approval = mockk<Approval>(relaxed = true)
        every { approval.data.payerId } returns mockPayerID
        every { approval.data.orderId } returns mockOrderID
        val onApproveSlot = slot<OnApprove>()
        val paypalCheckoutResultSlot = slot<PayPalNativeCheckoutResult>()

        every {
            PayPalCheckout.registerCallbacks(
                capture(onApproveSlot),
                any(),
                any(),
                any()
            )
        } answers { onApproveSlot.captured.onApprove(approval) }

        sut = getPayPalCheckoutClient()

        val payPalClientListener = mockk<PayPalNativeCheckoutListener>(relaxed = true)
        sut.listener = payPalClientListener

        every {
            payPalClientListener.onPayPalCheckoutSuccess(capture(paypalCheckoutResultSlot))
        } answers {
            assert(paypalCheckoutResultSlot.captured.orderID == mockOrderID)
            assert(paypalCheckoutResultSlot.captured.payerID == mockOrderID)
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

        val payPalClientListener = mockk<PayPalNativeCheckoutListener>(relaxed = true)
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

        val payPalClientListener = mockk<PayPalNativeCheckoutListener>(relaxed = true)
        sut.listener = payPalClientListener

        every {
            payPalClientListener.onPayPalCheckoutFailure(capture(paypalSdkErrorSlot))
        } answers {
            assert(paypalSdkErrorSlot.captured.errorDescription == errorMessage)
        }

        verify { payPalClientListener.onPayPalCheckoutFailure(any()) }
    }

    @Test
    fun `when OnShippingChange is invoked, onPayPalShippingChange is called`() {
        val onShippingChangeSlot = slot<OnShippingChange>()
        val shippingActionsSlot = slot<ShippingChangeActions>()
        val shippingDataSlot = slot<ShippingChangeData>()
        val shippingActions = mockk<ShippingChangeActions>()
        val shippingData = mockk<ShippingChangeData>()

        every {
            PayPalCheckout.registerCallbacks(
                any(),
                capture(onShippingChangeSlot),
                any(),
                any()
            )
        } answers { onShippingChangeSlot.captured.onShippingChanged(shippingData, shippingActions) }

        sut = getPayPalCheckoutClient()

        val payPalClientListener = mockk<PayPalNativeCheckoutListener>(relaxed = true)
        sut.listener = payPalClientListener

        every {
            payPalClientListener.onPayPalCheckoutShippingChange(capture(shippingDataSlot), capture(shippingActionsSlot))
        } answers {
            assert(shippingDataSlot.captured == shippingData)
            assert(shippingActionsSlot.captured == shippingActions)
        }

        verify { payPalClientListener.onPayPalCheckoutShippingChange(any(), any()) }
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
        coreConfig: CoreConfig = CoreConfig("fake-access-token"),
        testScheduler: TestCoroutineScheduler? = null
    ): PayPalNativeCheckoutClient {
        return testScheduler?.let {
            val dispatcher = StandardTestDispatcher(testScheduler)
            PayPalNativeCheckoutClient(mockApplication, coreConfig, mockReturnUrl, api, dispatcher)
        } ?: PayPalNativeCheckoutClient(mockApplication, coreConfig, mockReturnUrl, api)
    }
}
