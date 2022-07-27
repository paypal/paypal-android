package com.paypal.android.checkout

import android.app.Application
import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.Environment
import com.paypal.android.core.PayPalSDKError
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.createorder.CreateOrderActions
import com.paypal.checkout.error.ErrorInfo
import com.paypal.checkout.error.OnError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertSame
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
    private val coreConfig = CoreConfig(environment = Environment.SANDBOX)

    private val api = mockk<API>(relaxed = true)

    private lateinit var sut: PayPalClient

    @Before
    fun setUp() {
        mockkStatic(PayPalCheckout::class)
        every { PayPalCheckout.setConfig(any()) } just runs
        coEvery { api.getClientId() } returns mockClientId
    }

    @After
    fun dispose() {
        unmockkAll()
        resetField(PayPalCheckout::class.java, "isConfigSet", false)
    }

    @Test
    fun `when checkout is invoked, PayPalCheckout config is set`() = runBlocking {
        val configSlot = slot<CheckoutConfig>()
        every { PayPalCheckout.setConfig(capture(configSlot)) } answers { configSlot.captured }

        every {
            PayPalCheckout.start(any(), any(), any(), any(), any())
        } just runs

        sut = PayPalClient(mockApplication, coreConfig, mockReturnUrl, api)
        sut.checkout(generateRandomString())

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
    fun `when checkout is invoked, propagates core api client id fetch errors`() = runBlocking {
        val error = Exception("client id error")
        coEvery { api.getClientId() } throws error

        sut = PayPalClient(mockApplication, coreConfig, mockReturnUrl, api)

        var capturedError: Exception? = null
        try {
            sut.checkout(generateRandomString())
        } catch (e: Exception) {
            capturedError = e
        }
        assertSame(error, capturedError)
    }

    @Test
    fun `when checkout is invoked with LIVE env, PayPalCheckout config is set with LIVE`() =
        runBlocking {
            val configSlot = slot<CheckoutConfig>()
            every { PayPalCheckout.setConfig(capture(configSlot)) } answers { configSlot.captured }

            every {
                PayPalCheckout.start(any(), any(), any(), any(), any())
            } just runs

            val liveConfig = CoreConfig(environment = Environment.LIVE)
            sut = PayPalClient(mockApplication, liveConfig, mockReturnUrl, api)
            sut.checkout(generateRandomString())

            verify {
                PayPalCheckout.setConfig(any())
            }
            expectThat(configSlot.captured) {
                get { application }.isEqualTo(mockApplication)
                get { environment }.isEqualTo(com.paypal.checkout.config.Environment.LIVE)
            }
        }

    @Test
    fun `when checkout is invoked with STAGING env, PayPalCheckout config is set with STAGE`() =
        runBlocking {
            val configSlot = slot<CheckoutConfig>()
            every { PayPalCheckout.setConfig(capture(configSlot)) } answers { configSlot.captured }

            every {
                PayPalCheckout.start(any(), any(), any(), any(), any())
            } just runs

            val stagingConfig = CoreConfig(environment = Environment.STAGING)
            sut = PayPalClient(mockApplication, stagingConfig, mockReturnUrl, api)
            sut.checkout(generateRandomString())

            verify {
                PayPalCheckout.setConfig(any())
            }
            expectThat(configSlot.captured) {
                get { application }.isEqualTo(mockApplication)
                get { environment }.isEqualTo(com.paypal.checkout.config.Environment.STAGE)
            }
        }

    @Test
    fun `when checkout is invoked, PayPalCheckout start is called`() = runBlocking {
        every { PayPalCheckout.setConfig(any()) } just runs
        every { PayPalCheckout.start(any(), any(), any(), any(), any()) } just runs

        sut = PayPalClient(mockApplication, coreConfig, mockReturnUrl, api)
        sut.checkout(generateRandomString())

        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        verify {
            PayPalCheckout.start(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when checkout is invoked with an orderId, createOrderAction is called`() = runBlocking {
        val orderId = generateRandomString()
        val createOrderSlot = slot<CreateOrder>()
        val createOrderActions = mockk<CreateOrderActions>(relaxed = true)

        every {
            PayPalCheckout.start(
                capture(createOrderSlot),
                any(),
                any(),
                any(),
                any()
            )
        } answers { createOrderSlot.captured.create(createOrderActions) }

        sut = PayPalClient(mockApplication, coreConfig, mockReturnUrl, api)
        sut.checkout(orderId)

        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        verify {
            createOrderActions.set(orderId)
        }
    }

    @Test
    fun `when OnApprove is invoked, onPayPalSuccess is called`() = runBlocking {
        val orderId = generateRandomString()
        val payerId = generateRandomString()
        val paymentId = generateRandomString()
        val approval = mockk<com.paypal.checkout.approve.Approval>()
        val onApproveSlot = slot<OnApprove>()
        val paypalCheckoutResultSlot = slot<PayPalCheckoutResult>()

        val approvalDataMock = mockk<com.paypal.checkout.approve.ApprovalData>(relaxed = true)
        every { approvalDataMock.payerId } returns payerId
        every { approvalDataMock.orderId } returns orderId
        every { approvalDataMock.paymentId } returns paymentId

        every { approval.data } returns approvalDataMock

        every {
            PayPalCheckout.start(
                any(),
                capture(onApproveSlot),
                any(),
                any(),
                any()
            )
        } answers { onApproveSlot.captured.onApprove(approval) }

        sut = PayPalClient(mockApplication, coreConfig, mockReturnUrl, api)

        val payPalClientListener = mockk<PayPalListener>(relaxed = true)
        sut.listener = payPalClientListener

        every {
            payPalClientListener.onPayPalSuccess(capture(paypalCheckoutResultSlot))
        } answers {
            assert(paypalCheckoutResultSlot.captured.payerId == payerId)
            assert(paypalCheckoutResultSlot.captured.orderId == orderId)
        }

        sut.checkout(generateRandomString())
        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        verify { payPalClientListener.onPayPalSuccess(any()) }
    }

    @Test
    fun `when OnCancel is invoked, onPayPalCancel is called`() = runBlocking {
        val onCancelSlot = slot<OnCancel>()

        every {
            PayPalCheckout.start(
                any(),
                any(),
                any(),
                capture(onCancelSlot),
                any()
            )
        } answers { onCancelSlot.captured.onCancel() }

        sut = PayPalClient(mockApplication, coreConfig, mockReturnUrl, api)

        val payPalClientListener = mockk<PayPalListener>(relaxed = true)
        sut.listener = payPalClientListener

        sut.checkout(generateRandomString())
        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        verify { payPalClientListener.onPayPalCanceled() }
    }

    @Test
    fun `when OnError is invoked, onPayPalFailure is called`() = runBlocking {
        val errorMessage = "mock_error_message"
        val onError = slot<OnError>()
        val paypalSdkErrorSlot = slot<PayPalSDKError>()
        val errorInfo = mockk<ErrorInfo>(relaxed = true).also {
            every { it.reason }.returns(errorMessage)
        }

        every {
            PayPalCheckout.start(
                any(),
                any(),
                any(),
                any(),
                capture(onError)
            )
        } answers { onError.captured.onError(errorInfo) }

        sut = PayPalClient(mockApplication, coreConfig, mockReturnUrl, api)

        val payPalClientListener = mockk<PayPalListener>(relaxed = true)
        sut.listener = payPalClientListener

        every {
            payPalClientListener.onPayPalFailure(capture(paypalSdkErrorSlot))
        } answers {
            assert(paypalSdkErrorSlot.captured.errorDescription == errorMessage)
        }

        sut.checkout(generateRandomString())
        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        verify { payPalClientListener.onPayPalFailure(any()) }
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
}
