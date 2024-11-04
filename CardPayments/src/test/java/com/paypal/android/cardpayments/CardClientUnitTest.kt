package com.paypal.android.cardpayments

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.cardpayments.api.ConfirmPaymentSourceResponse
import com.paypal.android.corepayments.OrderStatus
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.isNull

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardClientUnitTest {

    private val card = Card("4111111111111111", "01", "24", "123")
    private val orderId = "sample-order-id"

    private val cardRequest = CardRequest(orderId, card, "merchant.app://return_url")
    private val cardVaultRequest =
        CardVaultRequest(setupTokenId = "fake-setup-token-id", card = card)

    private val cardAuthLauncher = mockk<CardAuthLauncher>(relaxed = true)

    private val checkoutOrdersAPI = mockk<CheckoutOrdersAPI>(relaxed = true)
    private val paymentMethodTokensAPI = mockk<DataVaultPaymentMethodTokensAPI>(relaxed = true)

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)
    private val confirmPaymentSourceResponse =
        ConfirmPaymentSourceResponse(orderId, OrderStatus.APPROVED)

    private val activity = mockk<FragmentActivity>(relaxed = true)

    private val approveOrderListener = mockk<ApproveOrderListener>(relaxed = true)
    private val cardVaultListener = mockk<CardVaultListener>(relaxed = true)

    private val intent = Intent()
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    // Ref: https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test#dispatchersmain-delegation
    private lateinit var mainThreadSurrogate: ExecutorCoroutineDispatcher

    @Before
    fun beforeEach() {
        mainThreadSurrogate = newSingleThreadContext("UI thread")
        Dispatchers.setMain(mainThreadSurrogate)

        every { activity.applicationContext } returns applicationContext
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun `approve order notifies listener of confirm payment source success`() = runTest {
        val sut = createCardClient(testScheduler)

        coEvery { checkoutOrdersAPI.confirmPaymentSource(cardRequest) } returns confirmPaymentSourceResponse

        sut.approveOrder(cardRequest)
        advanceUntilIdle()

        val resultSlot = slot<CardResult>()
        verify(exactly = 1) { approveOrderListener.onApproveOrderSuccess(capture(resultSlot)) }

        val actual = resultSlot.captured
        assertEquals("sample-order-id", actual.orderId)
        assertEquals(OrderStatus.APPROVED.name, actual.status)
        assertFalse(actual.didAttemptThreeDSecureAuthentication)
    }

    @Test
    fun `approve order notifies listener of confirm payment source failure`() = runTest {
        val sut = createCardClient(testScheduler)

        val error = PayPalSDKError(0, "mock_error_message")
        coEvery { checkoutOrdersAPI.confirmPaymentSource(cardRequest) } throws error

        sut.approveOrder(cardRequest)
        advanceUntilIdle()

        val errorSlot = slot<PayPalSDKError>()
        verify(exactly = 1) { approveOrderListener.onApproveOrderFailure(capture(errorSlot)) }

        val capturedError = errorSlot.captured
        assertEquals("mock_error_message", capturedError.errorDescription)
    }

    @Test
    fun `approveOrder() presents auth challenge when payer action is required`() = runTest {
        val threeDSecureAuthChallengeResponse =
            ConfirmPaymentSourceResponse(orderId, OrderStatus.APPROVED, "/payer/action/href")

        coEvery { checkoutOrdersAPI.confirmPaymentSource(cardRequest) } returns threeDSecureAuthChallengeResponse

        val sut = createCardClient(testScheduler)
        sut.approveOrder(cardRequest)
        advanceUntilIdle()

        val authChallengeSlot = slot<CardAuthChallenge>()
        verify { approveOrderListener.onAuthorizationRequired(capture(authChallengeSlot)) }

        val authChallenge = authChallengeSlot.captured as CardAuthChallenge.ApproveOrder
        assertEquals(Uri.parse("/payer/action/href"), authChallenge.url)
        assertEquals(cardRequest, authChallenge.request)
        assertEquals("merchant.app", authChallenge.returnUrlScheme)
    }

    @Test
    fun `vault notifies listener of update setup token success`() = runTest {
        val sut = createCardClient(testScheduler)

        val updateSetupTokenResult =
            UpdateSetupTokenResult("fake-setup-token-id-from-result", "fake-status", null)
        coEvery {
            paymentMethodTokensAPI.updateSetupToken(applicationContext, "fake-setup-token-id", card)
        } returns updateSetupTokenResult

        sut.vault(activity, cardVaultRequest)
        advanceUntilIdle()

        val resultSlot = slot<CardVaultResult>()
        verify(exactly = 1) { cardVaultListener.onVaultSuccess(capture(resultSlot)) }

        val actual = resultSlot.captured
        assertEquals("fake-setup-token-id-from-result", actual.setupTokenId)
        assertEquals("fake-status", actual.status)
        assertNull(actual.authChallenge)
    }

    @Test
    fun `vault notifies listener of update setup token failure`() = runTest {
        val sut = createCardClient(testScheduler)

        val error = PayPalSDKError(0, "mock_error_message")
        coEvery {
            paymentMethodTokensAPI.updateSetupToken(applicationContext, "fake-setup-token-id", card)
        } throws error

        sut.vault(activity, cardVaultRequest)
        advanceUntilIdle()

        val errorSlot = slot<PayPalSDKError>()
        verify(exactly = 1) { cardVaultListener.onVaultFailure(capture(errorSlot)) }

        val capturedError = errorSlot.captured
        assertEquals("mock_error_message", capturedError.errorDescription)
    }

    @Test
    fun `completeAuthChallenge() notifies merchant of approve order success`() = runTest {
        val sut = createCardClient(testScheduler)
        sut.approveOrderListener = approveOrderListener

        val successResult = CardResult(
            orderId = "fake-order-id",
            status = OrderStatus.APPROVED.name,
            didAttemptThreeDSecureAuthentication = false
        )
        every {
            cardAuthLauncher.completeAuthRequest(intent, "auth state")
        } returns CardStatus.ApproveOrderSuccess(successResult)

        sut.completeAuthChallenge(intent, "auth state")

        val slot = slot<CardResult>()
        verify(exactly = 1) { approveOrderListener.onApproveOrderSuccess(capture(slot)) }
        assertSame(successResult, slot.captured)
    }

    @Test
    fun `completeAuthChallenge() notifies merchant of approve order failure`() = runTest {
        val sut = createCardClient(testScheduler)
        sut.approveOrderListener = approveOrderListener

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            cardAuthLauncher.completeAuthRequest(intent, "auth state")
        } returns CardStatus.ApproveOrderError(error, "fake-order-id")

        sut.completeAuthChallenge(intent, "auth state")

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { approveOrderListener.onApproveOrderFailure(capture(slot)) }
        assertSame(error, slot.captured)
    }

    @Test
    fun `completeAuthChallenge() notifies merchant of approve order cancelation`() = runTest {
        val sut = createCardClient(testScheduler)
        sut.approveOrderListener = approveOrderListener

        every {
            cardAuthLauncher.completeAuthRequest(intent, "auth state")
        } returns CardStatus.ApproveOrderCanceled("fake-order-id")

        sut.completeAuthChallenge(intent, "auth state")
        verify(exactly = 1) { approveOrderListener.onApproveOrderCanceled() }
    }

    @Test
    fun `completeAuthChallenge() notifies merchant of vault success`() = runTest {
        val sut = createCardClient(testScheduler)
        sut.cardVaultListener = cardVaultListener

        val successResult = CardVaultResult("fake-setup-token-id", "fake-status")
        every {
            cardAuthLauncher.completeAuthRequest(intent, "auth state")
        } returns CardStatus.VaultSuccess(successResult)

        sut.completeAuthChallenge(intent, "auth state")

        val slot = slot<CardVaultResult>()
        verify(exactly = 1) { cardVaultListener.onVaultSuccess(capture(slot)) }
        assertSame(successResult, slot.captured)
    }

    @Test
    fun `completeAuthChallenge() notifies merchant of vault failure`() = runTest {
        val sut = createCardClient(testScheduler)
        sut.cardVaultListener = cardVaultListener

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            cardAuthLauncher.completeAuthRequest(intent, "auth state")
        } returns CardStatus.VaultError(error)

        sut.completeAuthChallenge(intent, "auth state")

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { cardVaultListener.onVaultFailure(capture(slot)) }
        assertSame(error, slot.captured)
    }

    @Test
    fun `completeAuthChallenge() notifies merchant of vault order cancelation`() = runTest {
        val sut = createCardClient(testScheduler)
        sut.cardVaultListener = cardVaultListener

        every {
            cardAuthLauncher.completeAuthRequest(intent, "auth state")
        } returns CardStatus.VaultCanceled("fake-setup-token-id")

        sut.completeAuthChallenge(intent, "auth state")
        // BREAKING CHANGE CALLOUT: if we introduce an "onVaultCanceled()" listener method, it could
        // break existing merchant integrations
        verify(exactly = 1) { cardVaultListener.onVaultFailure(any()) }
    }

    @Test
    fun `completeAuthChallenge() doesn't deliver result when browserSwitchResult is null`() =
        runTest {
            val sut = createCardClient(testScheduler)
            sut.approveOrderListener = approveOrderListener
            sut.cardVaultListener = cardVaultListener

            every {
                cardAuthLauncher.completeAuthRequest(intent, "auth state")
            } returns CardStatus.NoResult

            sut.completeAuthChallenge(intent, "auth state")
            verify { sut.approveOrderListener?.wasNot(Called) }
            verify { sut.cardVaultListener?.wasNot(Called) }
        }

    @Test
    fun `presentAuthChallenge() presents an approve order auth challenge using auth launcher`() =
        runTest {
            val sut = createCardClient(testScheduler)
            sut.approveOrderListener = approveOrderListener

            val url = Uri.parse("https://fake.com/url")
            val authChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)
            every {
                cardAuthLauncher.presentAuthChallenge(activity, authChallenge)
            } returns CardPresentAuthChallengeResult.Success("auth state")

            sut.presentAuthChallenge(activity, authChallenge)
            verify { cardAuthLauncher.presentAuthChallenge(activity, authChallenge) }
            verify(exactly = 0) { approveOrderListener.onApproveOrderFailure(any()) }
        }

    @Test
    fun `presentAuthChallenge() forwards approve order auth challenge presentation result to caller`() =
        runTest {
            val sut = createCardClient(testScheduler)
            sut.approveOrderListener = approveOrderListener

            val url = Uri.parse("https://fake.com/url")
            val authChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)

            val error = PayPalSDKError(123, "fake-error-description")
            val internalResult = CardPresentAuthChallengeResult.Failure(error)
            every {
                cardAuthLauncher.presentAuthChallenge(activity, authChallenge)
            } returns internalResult

            val resultReceivedByCaller = sut.presentAuthChallenge(activity, authChallenge)
            assertSame(internalResult, resultReceivedByCaller)
        }

    @Test
    fun `presentAuthChallenge() presents a vault auth challenge using auth launcher`() = runTest {
        val sut = createCardClient(testScheduler)
        sut.cardVaultListener = cardVaultListener

        val url = Uri.parse("https://fake.com/url")
        val authChallenge = CardAuthChallenge.Vault(url, cardVaultRequest)
        every {
            cardAuthLauncher.presentAuthChallenge(activity, authChallenge)
        } returns CardPresentAuthChallengeResult.Success("auth state")

        sut.presentAuthChallenge(activity, authChallenge)
        verify { cardAuthLauncher.presentAuthChallenge(activity, authChallenge) }
        verify(exactly = 0) { cardVaultListener.onVaultFailure(any()) }
    }

    @Test
    fun `presentAuthChallenge() forwards vault auth challenge presentation result to caller`() = runTest {
        val sut = createCardClient(testScheduler)
        sut.cardVaultListener = cardVaultListener

        val url = Uri.parse("https://fake.com/url")
        val authChallenge = CardAuthChallenge.Vault(url, cardVaultRequest)

        val error = PayPalSDKError(123, "fake-error-description")
        val internalResult = CardPresentAuthChallengeResult.Failure(error)
        every {
            cardAuthLauncher.presentAuthChallenge(activity, authChallenge)
        } returns internalResult

        val resultReceivedByCaller = sut.presentAuthChallenge(activity, authChallenge)
        assertSame(internalResult, resultReceivedByCaller)
    }

    private fun createCardClient(testScheduler: TestCoroutineScheduler): CardClient {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sut = CardClient(
            checkoutOrdersAPI,
            paymentMethodTokensAPI,
            analyticsService,
            cardAuthLauncher,
            dispatcher
        )
        sut.approveOrderListener = approveOrderListener
        sut.cardVaultListener = cardVaultListener
        return sut
    }

    @Test
    fun `when client is complete, all listeners are removed`() = runTest {
        val sut = createCardClient(testScheduler)

        val lifeCycle = mockk<Lifecycle>(relaxed = true)
        every { activity.lifecycle } returns lifeCycle

        sut.removeObservers()

        expectThat(sut.approveOrderListener).isNull()
        expectThat(sut.cardVaultListener).isNull()
    }
}
