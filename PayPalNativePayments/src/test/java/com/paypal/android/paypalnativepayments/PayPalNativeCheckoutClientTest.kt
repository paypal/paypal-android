package com.paypal.android.paypalnativepayments

import android.app.Application
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
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
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import java.lang.reflect.Field
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
import strikt.assertions.isTrue

@ExperimentalCoroutinesApi
class PayPalNativeCheckoutClientTest {

    private val mockApplication = mockk<Application>(relaxed = true)
    private val mockReturnUrl = "mock_return_url"

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)

    private lateinit var sut: PayPalNativeCheckoutClient

    @Before
    fun setUp() {
        mockkStatic(PayPalCheckout::class)
        every { PayPalCheckout.setConfig(any()) } just runs
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
        sut.startCheckout(PayPalNativeCheckoutRequest("order_id", "test@test.com"))
        advanceUntilIdle()

        verify {
            PayPalCheckout.setConfig(any())
        }
        expectThat(configSlot.captured) {
            get { clientId }.isEqualTo("fake-client-id")
            get { authConfig?.userEmail }.isEqualTo("test@test.com")
            get { application }.isEqualTo(mockApplication)
            get { environment }.isEqualTo(com.paypal.checkout.config.Environment.SANDBOX)
        }
    }

    @Test
    fun `when user location consent is set, startCheckout is called with the user location consent set`() =
        runTest {
            val userLocationConsentSlot = slot<Boolean>()
            every {
                PayPalCheckout.startCheckout(any(), capture(userLocationConsentSlot))
            } answers { userLocationConsentSlot.captured }

            sut = getPayPalCheckoutClient(testScheduler = testScheduler)
            sut.startCheckout(
                PayPalNativeCheckoutRequest(
                    "order_id",
                    "test@test.com",
                    true
                )
            )
            advanceUntilIdle()

            expectThat(userLocationConsentSlot.captured).isTrue()
        }

    @Test
    fun `when startCheckout is invoked with an invalid return_url, onPayPalCheckout failure is called`() =
        runTest {
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
    fun `when checkout is invoked with LIVE env, PayPalCheckout config is set with LIVE`() =
        runTest {
            val configSlot = slot<CheckoutConfig>()
            every { PayPalCheckout.setConfig(capture(configSlot)) } answers { configSlot.captured }

            every {
                PayPalCheckout.startCheckout(any())
            } just runs

            val config = CoreConfig("fake-client-id", Environment.LIVE)
            sut = getPayPalCheckoutClient(config, testScheduler)
            sut.startCheckout(PayPalNativeCheckoutRequest("order_id"))
            advanceUntilIdle()

            verify {
                PayPalCheckout.setConfig(any())
            }
            expectThat(configSlot.captured) {
                get { clientId }.isEqualTo("fake-client-id")
                get { application }.isEqualTo(mockApplication)
                get { environment }.isEqualTo(com.paypal.checkout.config.Environment.LIVE)
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
        val mockOrderId = "mock_order_id"
        val mockPayerId = "mock_payer_id"
        val approval = mockk<Approval>(relaxed = true)

        every { approval.data.payerId } returns mockPayerId
        every { approval.data.orderId } returns mockOrderId
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
                assertEquals(approve.orderId, mockOrderId)
                assertEquals(approve.payerId, mockPayerId)
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
        every { shippingData.payToken } returns "mock-order-id"

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
            analyticsService.sendAnalyticsEvent(
                "paypal-native-payments:shipping-address-changed",
                "mock-order-id"
            )
        }
    }

    @Test
    fun `when OnShippingChange is invoked with method change, onPayPalNativeShippingMethodChange is called`() =
        runTest {
            val onShippingChangeSlot = slot<OnShippingChange>()

            val mockId = "mock-id"
            val mockLabel = "mock_label"
            val shippingActions = mockk<ShippingChangeActions>(relaxed = true)
            val shippingData = mockk<ShippingChangeData>(relaxed = true)

            every { shippingData.shippingChangeType } returns ShippingChangeType.OPTION_CHANGE
            every { shippingData.selectedShippingOption } returns Options(mockId, true, mockLabel)
            every { shippingData.payToken } returns "mock-order-id"

            every {
                PayPalCheckout.registerCallbacks(
                    any(),
                    capture(onShippingChangeSlot),
                    any(),
                    any()
                )
            } answers {
                onShippingChangeSlot.captured.onShippingChanged(
                    shippingData,
                    shippingActions
                )
            }

            sut = getPayPalCheckoutClient()

            val payPalClientListener = mockk<PayPalNativeCheckoutListener>(relaxed = true)
            val shippingListener = mockk<PayPalNativeShippingListener>(relaxed = true)

            sut.shippingListener = shippingListener
            sut.listener = payPalClientListener

            verify {
                shippingListener.onPayPalNativeShippingMethodChange(
                    ofType(PayPalNativePaysheetActions::class),
                    withArg { option ->
                        assertEquals(option.id, mockId)
                        assertEquals(option.label, mockLabel)
                        assertTrue(option.selected)
                    }
                )
                analyticsService.sendAnalyticsEvent(
                    "paypal-native-payments:shipping-method-changed",
                    "mock-order-id"
                )
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
        coreConfig: CoreConfig = CoreConfig("fake-client-id"),
        testScheduler: TestCoroutineScheduler? = null
    ): PayPalNativeCheckoutClient {
        return testScheduler?.let {
            val dispatcher = StandardTestDispatcher(testScheduler)
            PayPalNativeCheckoutClient(
                mockApplication,
                coreConfig,
                mockReturnUrl,
                analyticsService,
                dispatcher
            )
        } ?: PayPalNativeCheckoutClient(
            mockApplication,
            coreConfig,
            mockReturnUrl,
            analyticsService,
        )
    }
}
