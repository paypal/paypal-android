package com.paypal.android.cardpayments

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.cardpayments.model.PaymentSource
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CardAuthLauncherUnitTest {

    private lateinit var browserSwitchClient: BrowserSwitchClient
    private lateinit var sut: CardAuthLauncher

    // TODO: consider using androidx.test activity instead of mockk
    // Ref: https://robolectric.org/androidx_test/#activities
    private val activity: FragmentActivity = mockk(relaxed = true)

    private val url = Uri.parse("https://fake.com/destination")
    private val card = Card("4111111111111111", "01", "25", "123")

    private val paymentSource = PaymentSource("1111", "Visa")
    private val approveOrderMetadata = ApproveOrderMetadata("sample-order-id", paymentSource)

    @Before
    fun beforeEach() {
        browserSwitchClient = mockk(relaxed = true)
    }

    @Test
    fun `presentAuthChallenge() returns an error when it cannot browser switch`() {
        val browserSwitchException = mockk<BrowserSwitchException>()
        every { browserSwitchException.message } returns "error message from browser switch"

        every { browserSwitchClient.start(any(), any()) } throws browserSwitchException

        val returnUrl = "merchant.app://return.com/destination"
        val cardRequest = CardRequest("fake-order-id", card, returnUrl)
        val cardAuthChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)

        sut = CardAuthLauncher(browserSwitchClient)
        val error = sut.presentAuthChallenge(activity, authChallenge = cardAuthChallenge)
        assertEquals("error message from browser switch", error?.errorDescription)
    }

    @Test
    fun `presentAuthChallenge() browser switches to approve order auth challenge url`() {
        val slot = slot<BrowserSwitchOptions>()
        every { browserSwitchClient.start(activity, capture(slot)) } just runs

        val returnUrl = "merchant.app://return.com/destination"
        val cardRequest = CardRequest("fake-order-id", card, returnUrl)
        val cardAuthChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)

        sut = CardAuthLauncher(browserSwitchClient)
        val error = sut.presentAuthChallenge(activity, authChallenge = cardAuthChallenge)
        assertNull(error)

        val browserSwitchOptions = slot.captured
        val metadata = browserSwitchOptions.metadata
        assertEquals("approve_order", metadata?.getString("request_type"))
        assertEquals("fake-order-id", metadata?.getString("order_id"))
        assertEquals("merchant.app", browserSwitchOptions.returnUrlScheme)
        assertEquals(Uri.parse("merchant.app://return.com/destination"), browserSwitchOptions.url)
    }

    @Test
    fun `presentAuthChallenge() browser switches to vault auth challenge url`() {
        val slot = slot<BrowserSwitchOptions>()
        every { browserSwitchClient.start(activity, capture(slot)) } just runs

        val returnUrl = "merchant.app://return.com/destination"
        val vaultRequest = CardVaultRequest("fake-setup-token-id", card, returnUrl)
        val vaultAuthRequest = CardAuthChallenge.Vault(url, vaultRequest)

        sut = CardAuthLauncher(browserSwitchClient)
        val error = sut.presentAuthChallenge(activity, authChallenge = vaultAuthRequest)
        assertNull(error)

        val browserSwitchOptions = slot.captured
        val metadata = browserSwitchOptions.metadata
        assertEquals("vault", metadata?.getString("request_type"))
        assertEquals("fake-setup-token-id", metadata?.getString("setup_token_id"))
        assertEquals("merchant.app", browserSwitchOptions.returnUrlScheme)
        assertEquals(Uri.parse("merchant.app://return.com/destination"), browserSwitchOptions.url)
    }

    // TODO: migrate from approve order flow
//    @Test
//    fun `handle browser switch result notifies user of success with liability shift`() =
//        runTest {
//            val sut = createCardClient(testScheduler)
//
//            val scheme = "com.paypal.android.demo"
//            val domain = "example.com"
//            val successDeepLink =
//                "$scheme://$domain/return_url?state=undefined&code=undefined&liability_shift=NO"
//
//            val browserSwitchResult = createBrowserSwitchResult(
//                BrowserSwitchStatus.SUCCESS,
//                approveOrderMetadata,
//                Uri.parse(successDeepLink)
//            )
//            every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult
//
//            sut.handleBrowserSwitchResult(activity)
//            advanceUntilIdle()
//
//            val cardResultSlot = slot<CardResult>()
//            coVerify(exactly = 1) {
//                approveOrderListener.onApproveOrderSuccess(capture(cardResultSlot))
//            }
//
//            val cardResult = cardResultSlot.captured
//            assertEquals("sample-order-id", cardResult.orderId)
//            assertEquals("NO", cardResult.liabilityShift)
//        }
    // TODO: migrate from approve order flow
//    @Test
//    fun `handle browser switch result notifies user of error when deep link contains one`() =
//        runTest {
//            val sut = createCardClient(testScheduler)
//
//            val scheme = "com.paypal.android.demo"
//            val domain = "example.com"
//            val successDeepLink = "$scheme://$domain/return_url?error=error"
//
//            val browserSwitchResult = createBrowserSwitchResult(
//                BrowserSwitchStatus.SUCCESS,
//                approveOrderMetadata,
//                Uri.parse(successDeepLink)
//            )
//            every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult
//
//            sut.handleBrowserSwitchResult(activity)
//            advanceUntilIdle()
//
//            val errorSlot = slot<PayPalSDKError>()
//            coVerify(exactly = 1) {
//                approveOrderListener.onApproveOrderFailure(capture(errorSlot))
//            }
//
//            val error = errorSlot.captured
//            assertEquals(0, error.code)
//            assertEquals("3DS Verification is returning an error.", error.errorDescription)
//        }
    // TODO: migrate from approve order flow
//    @Test
//    fun `handle browser switch result notifies user of error when deep link is null`() =
//        runTest {
//            val sut = createCardClient(testScheduler)
//            val browserSwitchResult = createBrowserSwitchResult(
//                BrowserSwitchStatus.SUCCESS,
//                approveOrderMetadata,
//                deepLinkUrl = null
//            )
//            every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult
//
//            sut.handleBrowserSwitchResult(activity)
//            advanceUntilIdle()
//
//            val errorSlot = slot<PayPalSDKError>()
//            coVerify(exactly = 1) {
//                approveOrderListener.onApproveOrderFailure(capture(errorSlot))
//            }
//
//            val error = errorSlot.captured
//            assertEquals(0, error.code)
//            assertEquals("3DS Verification is returning an error.", error.errorDescription)
//        }
    // TODO: migrate from approve order flow
//    @Test
//    fun `handle browser switch result notifies user of error when success deep link is missing code parameter`() =
//        runTest {
//            val sut = createCardClient(testScheduler)
//
//            val scheme = "com.paypal.android.demo"
//            val domain = "example.com"
//            val successDeepLink = "$scheme://$domain/return_url?state=undefined&liability_shift=NO"
//
//            val browserSwitchResult = createBrowserSwitchResult(
//                BrowserSwitchStatus.SUCCESS,
//                approveOrderMetadata,
//                Uri.parse(successDeepLink)
//            )
//            every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult
//
//            sut.handleBrowserSwitchResult(activity)
//            advanceUntilIdle()
//
//            val errorSlot = slot<PayPalSDKError>()
//            coVerify(exactly = 1) {
//                approveOrderListener.onApproveOrderFailure(capture(errorSlot))
//            }
//
//            val error = errorSlot.captured
//            assertEquals(1, error.code)
//            assertEquals("Malformed deeplink URL.", error.errorDescription)
//        }
    // TODO: migrate from approve order flow
//    @Test
//    fun `handle browser switch result notifies user of error when success deep link is missing state parameter`() =
//        runTest {
//            val sut = createCardClient(testScheduler)
//
//            val scheme = "com.paypal.android.demo"
//            val domain = "example.com"
//            val successDeepLink = "$scheme://$domain/return_url?code=undefined&liability_shift=NO"
//
//            val browserSwitchResult = createBrowserSwitchResult(
//                BrowserSwitchStatus.SUCCESS,
//                approveOrderMetadata,
//                Uri.parse(successDeepLink)
//            )
//            every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult
//
//            sut.handleBrowserSwitchResult(activity)
//            advanceUntilIdle()
//
//            val errorSlot = slot<PayPalSDKError>()
//            coVerify(exactly = 1) {
//                approveOrderListener.onApproveOrderFailure(capture(errorSlot))
//            }
//
//            val error = errorSlot.captured
//            assertEquals(1, error.code)
//            assertEquals("Malformed deeplink URL.", error.errorDescription)
//        }
    // TODO: migrate from approve order flow
//    @Test
//    fun `handle browser switch result notifies listener of cancelation`() = runTest {
//        val sut = createCardClient(testScheduler)
//
//        val browserSwitchResult = createBrowserSwitchResult(BrowserSwitchStatus.CANCELED)
//        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult
//
//        sut.handleBrowserSwitchResult(activity)
//        verify(exactly = 1) { approveOrderListener.onApproveOrderCanceled() }
//    }

    private fun createBrowserSwitchResult(
        @BrowserSwitchStatus status: Int,
        metadata: ApproveOrderMetadata? = null,
        deepLinkUrl: Uri? = null
    ): BrowserSwitchResult {

        val browserSwitchResult = mockk<BrowserSwitchResult>(relaxed = true)
        every { browserSwitchResult.status } returns status
        every { browserSwitchResult.deepLinkUrl } returns deepLinkUrl

        every { browserSwitchResult.requestMetadata } returns metadata?.toJSON()
        return browserSwitchResult
    }
}
