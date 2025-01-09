package com.paypal.android.cardpayments

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.cardpayments.analytics.CardAnalytics
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.cardpayments.api.ConfirmPaymentSourceResult
import com.paypal.android.corepayments.OrderStatus
import com.paypal.android.corepayments.PayPalSDKError
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
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardClientUnitTest {

    private val card = Card("4111111111111111", "01", "24", "123")
    private val orderId = "sample-order-id"

    private val cardRequest = CardRequest(orderId, card, "merchant.app://return_url")
    private val cardVaultRequest =
        CardVaultRequest(
            setupTokenId = "fake-setup-token-id",
            card = card,
            returnUrl = "merchant.app://return_url"
        )

    private val cardAuthLauncher = mockk<CardAuthLauncher>(relaxed = true)

    private val checkoutOrdersAPI = mockk<CheckoutOrdersAPI>(relaxed = true)
    private val paymentMethodTokensAPI = mockk<DataVaultPaymentMethodTokensAPI>(relaxed = true)

    private val cardAnalytics = mockk<CardAnalytics>(relaxed = true)
    private val confirmPaymentSourceResult =
        ConfirmPaymentSourceResult.Success(orderId, OrderStatus.APPROVED)

    private val activity = mockk<FragmentActivity>(relaxed = true)

    private val approveOrderCallback = mockk<CardApproveOrderCallback>()

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

        coEvery { checkoutOrdersAPI.confirmPaymentSource(cardRequest) } returns confirmPaymentSourceResult

        sut.approveOrder(cardRequest, approveOrderCallback)
        advanceUntilIdle()

        val resultSlot = slot<CardApproveOrderResult>()
        verify(exactly = 1) { approveOrderCallback.onCardApproveOrderResult(capture(resultSlot)) }

        val result = resultSlot.captured as CardApproveOrderResult.Success
        assertEquals("sample-order-id", result.orderId)
        assertEquals(OrderStatus.APPROVED.name, result.status)
        assertFalse(result.didAttemptThreeDSecureAuthentication)
    }

    @Test
    fun `approve order notifies listener of confirm payment source failure`() = runTest {
        val sut = createCardClient(testScheduler)

        val error = PayPalSDKError(0, "mock_error_message")
        coEvery {
            checkoutOrdersAPI.confirmPaymentSource(cardRequest)
        } returns ConfirmPaymentSourceResult.Failure(error)

        sut.approveOrder(cardRequest, approveOrderCallback)
        advanceUntilIdle()

        val resultSlot = slot<CardApproveOrderResult>()
        verify(exactly = 1) { approveOrderCallback.onCardApproveOrderResult(capture(resultSlot)) }

        val result = resultSlot.captured as CardApproveOrderResult.Failure
        assertEquals("mock_error_message", result.error.errorDescription)
    }

    @Test
    fun `approveOrder() notifies listener when authorization is required`() = runTest {
        val threeDSecureAuthChallengeResponse =
            ConfirmPaymentSourceResult.Success(orderId, OrderStatus.APPROVED, "/payer/action/href")

        coEvery { checkoutOrdersAPI.confirmPaymentSource(cardRequest) } returns threeDSecureAuthChallengeResponse

        val sut = createCardClient(testScheduler)
        sut.approveOrder(cardRequest, approveOrderCallback)
        advanceUntilIdle()

        val resultSlot = slot<CardApproveOrderResult>()
        verify(exactly = 1) { approveOrderCallback.onCardApproveOrderResult(capture(resultSlot)) }

        val result = resultSlot.captured as CardApproveOrderResult.AuthorizationRequired
        val authChallenge = result.authChallenge as CardAuthChallenge.ApproveOrder

        assertEquals(Uri.parse("/payer/action/href"), authChallenge.url)
        assertEquals(cardRequest, authChallenge.request)
        assertEquals("merchant.app", authChallenge.returnUrlScheme)
    }

    @Test
    fun `vault notifies listener of update setup token success`() = runTest {
        val sut = createCardClient(testScheduler)

        val updateSetupTokenResult =
            UpdateSetupTokenResult.Success("fake-setup-token-id-from-result", "fake-status", null)
        coEvery {
            paymentMethodTokensAPI.updateSetupToken("fake-setup-token-id", card)
        } returns updateSetupTokenResult

        val callback = mockk<CardVaultCallback>()
        sut.vault(cardVaultRequest, callback)
        advanceUntilIdle()

        val resultSlot = slot<CardVaultResult>()
        verify(exactly = 1) { callback.onCardVaultResult(capture(resultSlot)) }

        val result = resultSlot.captured as CardVaultResult.Success
        assertEquals("fake-setup-token-id-from-result", result.setupTokenId)
        assertEquals("fake-status", result.status)
    }

    @Test
    fun `vault notifies listener of update setup token failure`() = runTest {
        val sut = createCardClient(testScheduler)

        val error = PayPalSDKError(0, "mock_error_message")
        coEvery {
            paymentMethodTokensAPI.updateSetupToken("fake-setup-token-id", card)
        } returns UpdateSetupTokenResult.Failure(error)

        val callback = mockk<CardVaultCallback>()
        sut.vault(cardVaultRequest, callback)
        advanceUntilIdle()

        val resultSlot = slot<CardVaultResult>()
        verify(exactly = 1) { callback.onCardVaultResult(capture(resultSlot)) }

        val result = resultSlot.captured as CardVaultResult.Failure
        assertEquals("mock_error_message", result.error.errorDescription)
    }

    @Test
    fun `vault notifies listener when authorization is required`() = runTest {
        val sut = createCardClient(testScheduler)

        val updateSetupTokenResult = UpdateSetupTokenResult.Success(
            "fake-setup-token-id-from-result",
            "fake-status",
            "/payer/action/href"
        )
        coEvery {
            paymentMethodTokensAPI.updateSetupToken("fake-setup-token-id", card)
        } returns updateSetupTokenResult

        val callback = mockk<CardVaultCallback>()
        sut.vault(cardVaultRequest, callback)
        advanceUntilIdle()

        val resultSlot = slot<CardVaultResult>()
        verify(exactly = 1) { callback.onCardVaultResult(capture(resultSlot)) }

        val result = resultSlot.captured as CardVaultResult.AuthorizationRequired
        val authChallenge = result.authChallenge as CardAuthChallenge.Vault
        assertEquals(Uri.parse("/payer/action/href"), authChallenge.url)
        assertEquals(cardVaultRequest, authChallenge.request)
        assertEquals("merchant.app", authChallenge.returnUrlScheme)
    }

    @Test
    fun `completeApproveOrderAuthRequest() notifies merchant of approve order success`() = runTest {
        val sut = createCardClient(testScheduler)

        val successResult = CardFinishApproveOrderResult.Success(
            orderId = "fake-order-id",
            status = OrderStatus.APPROVED.name,
            didAttemptThreeDSecureAuthentication = false
        )
        every {
            cardAuthLauncher.completeApproveOrderAuthRequest(intent, "auth state")
        } returns successResult

        val actual = sut.finishApproveOrder(intent, "auth state")
        assertSame(successResult, actual)
    }

    @Test
    fun `completeApproveOrderAuthRequest() notifies merchant of approve order failure`() = runTest {
        val sut = createCardClient(testScheduler)

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = CardFinishApproveOrderResult.Failure(error)
        every {
            cardAuthLauncher.completeApproveOrderAuthRequest(intent, "auth state")
        } returns failureResult

        val actual = sut.finishApproveOrder(intent, "auth state")
        assertSame(failureResult, actual)
    }

    @Test
    fun `completeAuthChallenge() notifies merchant of approve order cancelation`() = runTest {
        val sut = createCardClient(testScheduler)

        val canceledResult = CardFinishApproveOrderResult.Canceled
        every {
            cardAuthLauncher.completeApproveOrderAuthRequest(intent, "auth state")
        } returns canceledResult

        val result = sut.finishApproveOrder(intent, "auth state")
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishVault() forwards result from auth launcher`() = runTest {
        val sut = createCardClient(testScheduler)

        val successResult =
            CardFinishVaultResult.Success("fake-setup-token-id", "fake-status")
        every {
            cardAuthLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns successResult

        val result = sut.finishVault(intent, "auth state")
        assertSame(successResult, result)
    }

    @Test
    fun `presentAuthChallenge() presents an approve order auth challenge using auth launcher`() =
        runTest {
            val sut = createCardClient(testScheduler)

            val url = Uri.parse("https://fake.com/url")
            val authChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)
            every {
                cardAuthLauncher.presentAuthChallenge(activity, authChallenge)
            } returns CardPresentAuthChallengeResult.Success("auth state")

            sut.presentAuthChallenge(activity, authChallenge)
            verify { cardAuthLauncher.presentAuthChallenge(activity, authChallenge) }
        }

    @Test
    fun `presentAuthChallenge() forwards approve order auth challenge presentation result to caller`() =
        runTest {
            val sut = createCardClient(testScheduler)

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

        val url = Uri.parse("https://fake.com/url")
        val authChallenge = CardAuthChallenge.Vault(url, cardVaultRequest)
        every {
            cardAuthLauncher.presentAuthChallenge(activity, authChallenge)
        } returns CardPresentAuthChallengeResult.Success("auth state")

        sut.presentAuthChallenge(activity, authChallenge)
        verify { cardAuthLauncher.presentAuthChallenge(activity, authChallenge) }
    }

    @Test
    fun `presentAuthChallenge() forwards vault auth challenge presentation result to caller`() =
        runTest {
            val sut = createCardClient(testScheduler)

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
            cardAnalytics,
            cardAuthLauncher,
            dispatcher
        )
        return sut
    }
}
