package com.paypal.android.cardpayments

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private val url = Uri.parse("https://fake.com/destination")
    private val card = Card("4111111111111111", "01", "25", "123")

    private val approveOrderMetadata = JSONObject()
        .put("request_type", "approve_order")
        .put("order_id", "fake-order-id")

    private val vaultMetadata = JSONObject()
        .put("request_type", "vault")
        .put("setup_token_id", "fake-setup-token-id")

    @Before
    fun beforeEach() {
        browserSwitchClient = mockk(relaxed = true)
    }

    @Test
    fun `presentAuthChallenge() returns an error for approve order when it cannot browser switch`() {
        val browserSwitchException = mockk<BrowserSwitchException>()
        every { browserSwitchException.message } returns "error message from browser switch"

        every { browserSwitchClient.start(any(), any()) } throws browserSwitchException

        val returnUrl = "merchant.app://return.com/deep-link"
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

        val returnUrl = "merchant.app://return.com/deep-link"
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
        assertEquals(Uri.parse("https://fake.com/destination"), browserSwitchOptions.url)
    }

    @Test
    fun `presentAuthChallenge() browser switches to vault auth challenge url`() {
        val slot = slot<BrowserSwitchOptions>()
        every { browserSwitchClient.start(activity, capture(slot)) } just runs

        val returnUrl = "merchant.app://return.com/deep-link"
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
        assertEquals(Uri.parse("https://fake.com/destination"), browserSwitchOptions.url)
    }

    @Test
    fun `deliverBrowserSwitchResult() returns approve order success`() {
        sut = CardAuthLauncher(browserSwitchClient)

        val scheme = "com.paypal.android.demo"
        val domain = "example.com"
        val successDeepLink =
            "$scheme://$domain/return_url?state=undefined&code=undefined&liability_shift=NO"

        val browserSwitchResult = createBrowserSwitchResult(
            BrowserSwitchStatus.SUCCESS,
            approveOrderMetadata,
            Uri.parse(successDeepLink)
        )
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        val status = sut.deliverBrowserSwitchResult(activity) as CardStatus.ApproveOrderSuccess
        val cardResult = status.result
        assertEquals("fake-order-id", cardResult.orderId)
        assertTrue(cardResult.didAttemptThreeDSecureAuthentication)
        assertNull(cardResult.status)
    }

    @Test
    fun `deliverBrowserSwitchResult() returns approve order canceled when browser switch was canceled`() {
        sut = CardAuthLauncher(browserSwitchClient)

        val browserSwitchResult =
            createBrowserSwitchResult(BrowserSwitchStatus.CANCELED, approveOrderMetadata)
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        val status = sut.deliverBrowserSwitchResult(activity) as CardStatus.ApproveOrderCanceled
        assertEquals("fake-order-id", status.orderId)
    }

    @Test
    fun `deliverBrowserSwitchResult() returns vault success`() {
        sut = CardAuthLauncher(browserSwitchClient)

        val scheme = "com.paypal.android.demo"
        val domain = "example.com"
        val successDeepLink = "$scheme://$domain/success"

        val browserSwitchResult = createBrowserSwitchResult(
            BrowserSwitchStatus.SUCCESS,
            vaultMetadata,
            Uri.parse(successDeepLink)
        )
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        val status = sut.deliverBrowserSwitchResult(activity) as CardStatus.VaultSuccess
        val vaultResult = status.result
        assertEquals("fake-setup-token-id", vaultResult.setupTokenId)
        assertEquals("SCA_COMPLETE", vaultResult.status)
    }

    private fun createBrowserSwitchResult(
        @BrowserSwitchStatus status: Int,
        metadata: JSONObject? = null,
        deepLinkUrl: Uri? = null
    ): BrowserSwitchResult {

        val browserSwitchResult = mockk<BrowserSwitchResult>(relaxed = true)
        every { browserSwitchResult.status } returns status
        every { browserSwitchResult.deepLinkUrl } returns deepLinkUrl

        every { browserSwitchResult.requestMetadata } returns metadata
        return browserSwitchResult
    }
}
