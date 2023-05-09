package com.paypal.android.paypalnativepayments

import android.app.Application
import com.paypal.android.corepayments.API
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
import com.paypal.checkout.order.Options
import com.paypal.checkout.shipping.OnShippingChange
import com.paypal.checkout.shipping.ShippingChangeActions
import com.paypal.checkout.shipping.ShippingChangeAddress
import com.paypal.checkout.shipping.ShippingChangeData
import com.paypal.checkout.shipping.ShippingChangeType
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        sut.startCheckout(PayPalNativeCheckoutRequest("order_id"))
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

        sut.startCheckout(PayPalNativeCheckoutRequest("order_id"))
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
            sut.startCheckout(PayPalNativeCheckoutRequest("order_id"))
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
            sut.startCheckout(PayPalNativeCheckoutRequest("order_id"))
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

        verify {
            payPalClientListener.onPayPalCheckoutSuccess(withArg { approve ->
                assertEquals(approve.orderID, mockOrderID)
                assertEquals(approve.payerID, mockPayerID)
            })
        }
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

        verify {
            payPalClientListener.onPayPalCheckoutFailure(withArg { error ->
                assertEquals(error.errorDescription, errorMessage)
            })
        }
    }

    @Test
    fun `when OnShippingChange is invoked with address change, onPayPalNativeShippingAddressChange is called`() {
        val onShippingChangeSlot = slot<OnShippingChange>()
        val mockCountryCode = "mock_country_code"
        val shippingActions = mockk<ShippingChangeActions>(relaxed = true)
        val shippingData = mockk<ShippingChangeData>(relaxed = true)

        every { shippingData.shippingChangeType } returns ShippingChangeType.ADDRESS_CHANGE
        every { shippingData.shippingAddress } returns ShippingChangeAddress(countryCode = mockCountryCode)

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
        val shippingListener = mockk<PayPalNativeShippingListener>(relaxed = true)
        sut.shippingListener = shippingListener
        sut.listener = payPalClientListener

        verify {
            shippingListener.onPayPalNativeShippingAddressChange(
                ofType(PayPalNativePaysheetActions::class),
                withArg { address ->
                    assertEquals(address.countryCode, mockCountryCode)
                }
            )
            api.sendAnalyticsEvent("paypal-native-payments:shipping-address-changed", null)
        }
    }

    @Test
    fun `when OnShippingChange is invoked with method change, onPayPalNativeShippingMethodChange is called`() =
        runTest {
            val onShippingChangeSlot = slot<OnShippingChange>()

            val mockID = "mock_ID"
            val mockLabel = "mock_label"
            val shippingActions = mockk<ShippingChangeActions>(relaxed = true)
            val shippingData = mockk<ShippingChangeData>(relaxed = true)

            every { shippingData.shippingChangeType } returns ShippingChangeType.OPTION_CHANGE
            every { shippingData.selectedShippingOption } returns Options(mockID, true, mockLabel)

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
            val shippingListener = mockk<PayPalNativeShippingListener>(relaxed = true)

            sut.shippingListener = shippingListener
            sut.listener = payPalClientListener

            verify {
                shippingListener.onPayPalNativeShippingMethodChange(
                    ofType(PayPalNativePaysheetActions::class),
                        withArg { option ->
                        assertEquals(option.id, mockID)
                        assertEquals(option.label, mockLabel)
                        assertTrue(option.selected)
                    }
                )
                api.sendAnalyticsEvent("paypal-native-payments:shipping-method-changed", null)
            }
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
