package com.paypal.android.checkout

import android.app.Application
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
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.lang.reflect.Field

class PayPalClientTest {

    private val mockApplication = mockk<Application>(relaxed = true)
    private val mockClientId = generateRandomString()
    private val mockReturnUrl = "com.example://paypalpay"
    private val coreConfig = CoreConfig(environment = Environment.SANDBOX)

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
    fun `when PayPalClient is instantiated, PayPalCheckout config is set`() {
        val configSlot = slot<CheckoutConfig>()

        every { PayPalCheckout.setConfig(capture(configSlot)) } answers { configSlot.captured }
        PayPalClient(mockApplication, coreConfig, mockReturnUrl, mockClientId)

        verify {
            PayPalCheckout.setConfig(any())
        }
        expectThat(configSlot.captured) {
            get { application }.isEqualTo(mockApplication)
            get { environment }.isEqualTo(com.paypal.checkout.config.Environment.SANDBOX)
        }
    }

    @Test
    fun `when PayPalClient is instantiated with LIVE, PayPalCheckout config is set with LIVE`() {
        val configSlot = slot<CheckoutConfig>()

        every { PayPalCheckout.setConfig(capture(configSlot)) } answers { configSlot.captured }
        PayPalClient(mockApplication,
            CoreConfig(environment = Environment.LIVE),
            mockReturnUrl,
            mockClientId
        )

        verify {
            PayPalCheckout.setConfig(any())
        }
        expectThat(configSlot.captured) {
            get { application }.isEqualTo(mockApplication)
            get { environment }.isEqualTo(com.paypal.checkout.config.Environment.LIVE)
        }
    }

    @Test
    fun `when PayPalClient is instantiated with STAGING, PayPalCheckout config is set with STAGE`() {
        val configSlot = slot<CheckoutConfig>()

        every { PayPalCheckout.setConfig(capture(configSlot)) } answers { configSlot.captured }
        PayPalClient(
            mockApplication,
            CoreConfig(environment = Environment.STAGING),
            mockReturnUrl,
            mockClientId
        )

        verify {
            PayPalCheckout.setConfig(any())
        }
        expectThat(configSlot.captured) {
            get { application }.isEqualTo(mockApplication)
            get { environment }.isEqualTo(com.paypal.checkout.config.Environment.STAGE)
        }
    }

    @Test
    fun `when checkout is invoked, PayPalCheckout start is called`() {
        every { PayPalCheckout.setConfig(any()) } just runs
        every { PayPalCheckout.start(any(), any(), any(), any(), any()) } just runs

        val paypalClient = PayPalClient(mockApplication, coreConfig, mockReturnUrl, mockClientId)

        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        paypalClient.checkout(generateRandomString())

        verify {
            PayPalCheckout.start(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when checkout is invoked with an orderId, createOrderAction is called`() {
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

        val paypalClient = PayPalClient(mockApplication, coreConfig, mockReturnUrl, mockClientId)
        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        paypalClient.checkout(orderId)

        verify {
            createOrderActions.set(orderId)
        }
    }

    @Test
    fun `when OnApprove is invoked, onPayPalSuccess is called`() {
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

        val paypalClient = PayPalClient(mockApplication, coreConfig, mockReturnUrl, mockClientId)
        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        val payPalClientListener = mockk<PayPalListener>(relaxed = true)
        paypalClient.listener = payPalClientListener

        every {
            payPalClientListener.onPayPalSuccess(capture(paypalCheckoutResultSlot))
        } answers {
            assert(paypalCheckoutResultSlot.captured.payerId == payerId)
            assert(paypalCheckoutResultSlot.captured.orderId == orderId)
        }

        paypalClient.checkout(generateRandomString())
    }

    @Test
    fun `when OnCancel is invoked, onPayPalCancel is called`() {
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

        val paypalClient = PayPalClient(mockApplication, coreConfig, mockReturnUrl, mockClientId)
        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        val payPalClientListener = mockk<PayPalListener>(relaxed = true)

        paypalClient.listener = payPalClientListener

        paypalClient.checkout(generateRandomString())

        verify {
            payPalClientListener.onPayPalCanceled()
        }
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
            PayPalCheckout.start(
                any(),
                any(),
                any(),
                any(),
                capture(onError)
            )
        } answers { onError.captured.onError(errorInfo) }

        val paypalClient = PayPalClient(mockApplication, coreConfig, mockReturnUrl, mockClientId)

        val payPalClientListener = mockk<PayPalListener>(relaxed = true)
        paypalClient.listener = payPalClientListener

        every {
            payPalClientListener.onPayPalFailure(capture(paypalSdkErrorSlot))
        } answers {
            assert(paypalSdkErrorSlot.captured.errorDescription == errorMessage)
        }

        resetField(PayPalCheckout::class.java, "isConfigSet", true)

        paypalClient.checkout(generateRandomString())
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
