package com.paypal.android.cardpayments

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchStartResult
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardAuthLauncherUnitTest {

    private lateinit var browserSwitchClient: BrowserSwitchClient
    private lateinit var sut: CardAuthLauncher

    // TODO: consider using androidx.test activity instead of mockk
    // Ref: https://robolectric.org/androidx_test/#activities
    private val activity: FragmentActivity = mockk(relaxed = true)
    private val intent: Intent = Intent()

    private val url = Uri.parse("https://fake.com/destination")
    private val card = Card("4111111111111111", "01", "25", "123")

    private val approveOrderMetadata = JSONObject()
        .put("order_id", "fake-order-id")

    private val vaultMetadata = JSONObject()
        .put("setup_token_id", "fake-setup-token-id")

    @Before
    fun beforeEach() {
        browserSwitchClient = mockk(relaxed = true)
    }

    @Test
    fun `presentAuthChallenge() returns an error for approve order when it cannot browser switch`() {
        val browserSwitchResult =
            BrowserSwitchStartResult.Failure(Exception("error message from browser switch"))
        every { browserSwitchClient.start(any(), any()) } returns browserSwitchResult

        val returnUrl = "merchant.app://return.com/deep-link"
        val cardRequest = CardRequest("fake-order-id", card, returnUrl)
        val cardAuthChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)

        sut = CardAuthLauncher(browserSwitchClient)
        val result = sut.presentAuthChallenge(activity, authChallenge = cardAuthChallenge)
                as CardPresentAuthChallengeResult.Failure
        assertEquals("error message from browser switch", result.error.errorDescription)
    }

    @Test
    fun `presentAuthChallenge() browser switches to approve order auth challenge url`() {
        val slot = slot<BrowserSwitchOptions>()
        val browserSwitchResult = BrowserSwitchStartResult.Started("pending request")
        every { browserSwitchClient.start(activity, capture(slot)) } returns browserSwitchResult

        val returnUrl = "merchant.app://return.com/deep-link"
        val cardRequest = CardRequest("fake-order-id", card, returnUrl)
        val cardAuthChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)

        sut = CardAuthLauncher(browserSwitchClient)
        val status = sut.presentAuthChallenge(activity, authChallenge = cardAuthChallenge)
        assertTrue(status is CardPresentAuthChallengeResult.Success)

        val browserSwitchOptions = slot.captured
        val metadata = browserSwitchOptions.metadata
        assertEquals("fake-order-id", metadata?.getString("order_id"))
        assertEquals("merchant.app", browserSwitchOptions.returnUrlScheme)
        assertEquals(Uri.parse("https://fake.com/destination"), browserSwitchOptions.url)
        assertEquals(BrowserSwitchRequestCodes.CARD_APPROVE_ORDER, browserSwitchOptions.requestCode)
    }

    @Test
    fun `presentAuthChallenge() browser switches to vault auth challenge url`() {
        val slot = slot<BrowserSwitchOptions>()
        val browserSwitchResult = BrowserSwitchStartResult.Started("pending request")
        every { browserSwitchClient.start(activity, capture(slot)) } returns browserSwitchResult

        val returnUrl = "merchant.app://return.com/deep-link"
        val vaultRequest = CardVaultRequest("fake-setup-token-id", card, returnUrl)
        val vaultAuthRequest = CardAuthChallenge.Vault(url, vaultRequest)

        sut = CardAuthLauncher(browserSwitchClient)
        val status = sut.presentAuthChallenge(activity, authChallenge = vaultAuthRequest)
        assertTrue(status is CardPresentAuthChallengeResult.Success)

        val browserSwitchOptions = slot.captured
        val metadata = browserSwitchOptions.metadata
        assertEquals("fake-setup-token-id", metadata?.getString("setup_token_id"))
        assertEquals("merchant.app", browserSwitchOptions.returnUrlScheme)
        assertEquals(Uri.parse("https://fake.com/destination"), browserSwitchOptions.url)
        assertEquals(BrowserSwitchRequestCodes.CARD_VAULT, browserSwitchOptions.requestCode)
    }

    @Test
    fun `completeAuthRequest() returns unknown error when browser switch fails`() {
        sut = CardAuthLauncher(browserSwitchClient)

        val browserSwitchError = BrowserSwitchException("browser switch error")
        val finalResult = mockk<BrowserSwitchFinalResult.Failure>(relaxed = true)
        every { finalResult.error } returns browserSwitchError

        every {
            browserSwitchClient.completeRequest(intent, "pending request")
        } returns finalResult

        val status = sut.completeAuthRequest(intent, "pending request")
                as CardStatus.UnknownError
        assertSame(browserSwitchError, status.error)
    }

    @Test
    fun `completeAuthRequest() returns no result when request code is not for card`() {
        sut = CardAuthLauncher(browserSwitchClient)

        val scheme = "com.paypal.android.demo"
        val domain = "example.com"
        val successDeepLink =
            "$scheme://$domain/return_url?state=undefined&code=undefined&liability_shift=NO"

        val finalResult = createBrowserSwitchSuccessFinalResult(
            BrowserSwitchRequestCodes.PAYPAL_CHECKOUT,
            approveOrderMetadata,
            Uri.parse(successDeepLink)
        )
        every {
            browserSwitchClient.completeRequest(intent, "pending request")
        } returns finalResult

        val status = sut.completeAuthRequest(intent, "pending request")
        assertTrue(status is CardStatus.NoResult)
    }

    @Test
    fun `completeAuthRequest() returns approve order success`() {
        sut = CardAuthLauncher(browserSwitchClient)

        val scheme = "com.paypal.android.demo"
        val domain = "example.com"
        val successDeepLink =
            "$scheme://$domain/return_url?state=undefined&code=undefined&liability_shift=NO"

        val finalResult = createBrowserSwitchSuccessFinalResult(
            BrowserSwitchRequestCodes.CARD_APPROVE_ORDER,
            approveOrderMetadata,
            Uri.parse(successDeepLink)
        )
        every {
            browserSwitchClient.completeRequest(intent, "pending request")
        } returns finalResult

        val status = sut.completeAuthRequest(intent, "pending request")
                as CardStatus.ApproveOrderSuccess

        val cardResult = status.result
        assertEquals("fake-order-id", cardResult.orderId)
        assertTrue(cardResult.didAttemptThreeDSecureAuthentication)
        assertNull(cardResult.status)
    }

    @Test
    fun `completeAuthRequest() returns vault success`() {
        sut = CardAuthLauncher(browserSwitchClient)

        val scheme = "com.paypal.android.demo"
        val domain = "example.com"
        val successDeepLink = "$scheme://$domain/success"

        val finalResult = createBrowserSwitchSuccessFinalResult(
            BrowserSwitchRequestCodes.CARD_VAULT,
            vaultMetadata,
            Uri.parse(successDeepLink)
        )
        every {
            browserSwitchClient.completeRequest(intent, "pending request")
        } returns finalResult

        val status = sut.completeAuthRequest(intent, "pending request") as CardStatus.VaultSuccess
        val vaultResult = status.result
        assertEquals("fake-setup-token-id", vaultResult.setupTokenId)
        assertNull(vaultResult.status)
    }

    private fun createBrowserSwitchSuccessFinalResult(
        requestCode: Int,
        metadata: JSONObject,
        deepLinkUrl: Uri
    ): BrowserSwitchFinalResult.Success {
        val finalResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { finalResult.returnUrl } returns deepLinkUrl
        every { finalResult.requestMetadata } returns metadata
        every { finalResult.requestCode } returns requestCode
        every { finalResult.requestUrl } returns Uri.parse("https://example.com/url")
        return finalResult
    }
}
