package com.paypal.android.cardpayments

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStartResult
import io.mockk.every
import io.mockk.mockk
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
        every {
            browserSwitchClient.start(any<FragmentActivity>(), any<BrowserSwitchOptions>())
        } returns browserSwitchResult

        val returnUrl = "merchant.app://return.com/deep-link"
        val cardRequest = CardRequest("fake-order-id", card, returnUrl)
        val cardAuthChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)

        sut = CardAuthLauncher(browserSwitchClient)
        val result = sut.presentAuthChallenge(activity, authChallenge = cardAuthChallenge)
                as CardPresentAuthChallengeResult.Failure
        assertEquals("error message from browser switch", result.error.errorDescription)
    }

    @Test
    fun `presentAuthChallenge() browser switches to approve order auth challenge url with custom scheme return url`() {
        val slot = slot<BrowserSwitchOptions>()
        val browserSwitchResult = BrowserSwitchStartResult.Success
        every { browserSwitchClient.start(activity, capture(slot)) } returns browserSwitchResult

        val customSchemeReturnUrl = "merchant.app://return.com/deep-link"
        val cardRequest = CardRequest("fake-order-id", card, customSchemeReturnUrl)
        val cardAuthChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)

        sut = CardAuthLauncher(browserSwitchClient)
        val status = sut.presentAuthChallenge(activity, authChallenge = cardAuthChallenge)
        assertTrue(status is CardPresentAuthChallengeResult.Success)

        val browserSwitchOptions = slot.captured
        val metadata = browserSwitchOptions.metadata
        assertEquals("fake-order-id", metadata?.getString("order_id"))
        assertEquals("merchant.app", browserSwitchOptions.returnUrlScheme)
        assertNull(browserSwitchOptions.appLinkUrl)
        assertEquals(Uri.parse("https://fake.com/destination"), browserSwitchOptions.targetUri)
        assertEquals(BrowserSwitchRequestCodes.CARD_APPROVE_ORDER, browserSwitchOptions.requestCode)
    }

    @Test
    fun `presentAuthChallenge() browser switches to approve order auth challenge url with app link return url`() {
        val slot = slot<BrowserSwitchOptions>()
        val browserSwitchResult = BrowserSwitchStartResult.Success
        every { browserSwitchClient.start(activity, capture(slot)) } returns browserSwitchResult

        val appLinkReturnUrl = "https://merchant.com/app-link"
        val cardRequest = CardRequest("fake-order-id", card, appLinkReturnUrl)
        val cardAuthChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)

        sut = CardAuthLauncher(browserSwitchClient)
        val status = sut.presentAuthChallenge(activity, authChallenge = cardAuthChallenge)
        assertTrue(status is CardPresentAuthChallengeResult.Success)

        val browserSwitchOptions = slot.captured
        val metadata = browserSwitchOptions.metadata
        assertEquals("fake-order-id", metadata?.getString("order_id"))
        assertNull(browserSwitchOptions.returnUrlScheme)
        assertEquals("https://merchant.com/app-link", browserSwitchOptions.appLinkUrl)
        assertEquals(Uri.parse("https://fake.com/destination"), browserSwitchOptions.targetUri)
        assertEquals(BrowserSwitchRequestCodes.CARD_APPROVE_ORDER, browserSwitchOptions.requestCode)
    }

    @Test
    fun `presentAuthChallenge() browser switches to vault auth challenge url with custom scheme return url`() {
        val slot = slot<BrowserSwitchOptions>()
        val browserSwitchResult = BrowserSwitchStartResult.Success
        every { browserSwitchClient.start(activity, capture(slot)) } returns browserSwitchResult

        val customSchemeReturnUrl = "merchant.app://return.com/deep-link"
        val vaultRequest = CardVaultRequest("fake-setup-token-id", card, customSchemeReturnUrl)
        val vaultAuthRequest = CardAuthChallenge.Vault(url, vaultRequest)

        sut = CardAuthLauncher(browserSwitchClient)
        val status = sut.presentAuthChallenge(activity, authChallenge = vaultAuthRequest)
        assertTrue(status is CardPresentAuthChallengeResult.Success)

        val browserSwitchOptions = slot.captured
        val metadata = browserSwitchOptions.metadata
        assertEquals("fake-setup-token-id", metadata?.getString("setup_token_id"))
        assertEquals("merchant.app", browserSwitchOptions.returnUrlScheme)
        assertEquals(Uri.parse("https://fake.com/destination"), browserSwitchOptions.targetUri)
        assertEquals(BrowserSwitchRequestCodes.CARD_VAULT, browserSwitchOptions.requestCode)
    }

    @Test
    fun `presentAuthChallenge() browser switches to vault auth challenge url with app links return url`() {
        val slot = slot<BrowserSwitchOptions>()
        val browserSwitchResult = BrowserSwitchStartResult.Success
        every { browserSwitchClient.start(activity, capture(slot)) } returns browserSwitchResult

        val appLinksReturnUrl = "https://merchant.com/app-link"
        val vaultRequest = CardVaultRequest("fake-setup-token-id", card, appLinksReturnUrl)
        val vaultAuthRequest = CardAuthChallenge.Vault(url, vaultRequest)

        sut = CardAuthLauncher(browserSwitchClient)
        val status = sut.presentAuthChallenge(activity, authChallenge = vaultAuthRequest)
        assertTrue(status is CardPresentAuthChallengeResult.Success)

        val browserSwitchOptions = slot.captured
        val metadata = browserSwitchOptions.metadata
        assertEquals("fake-setup-token-id", metadata?.getString("setup_token_id"))
        assertNull(browserSwitchOptions.returnUrlScheme)
        assertEquals("https://merchant.com/app-link", browserSwitchOptions.appLinkUrl)
        assertEquals(Uri.parse("https://fake.com/destination"), browserSwitchOptions.targetUri)
        assertEquals(BrowserSwitchRequestCodes.CARD_VAULT, browserSwitchOptions.requestCode)
    }

    @Test
    fun `completeApproveOrderAuthRequest() returns approve order success`() {
        sut = CardAuthLauncher(browserSwitchClient)

        val scheme = "com.paypal.android.demo"
        val domain = "example.com"
        val successDeepLink =
            "$scheme://$domain/return_url?state=undefined&code=undefined&liability_shift=NO"
        val options = BrowserSwitchOptions(
            targetUri = "https://fake.com/destination".toUri(),
            requestCode = BrowserSwitchRequestCodes.CARD_APPROVE_ORDER,
            returnUrlScheme = scheme,
            appLinkUrl = null,
            metadata = approveOrderMetadata
        )
        val authState = BrowserSwitchPendingState(options).toBase64EncodedJSON()
        intent.data = successDeepLink.toUri()

        val result = sut.completeApproveOrderAuthRequest(intent, authState)
                as CardFinishApproveOrderResult.Success

        assertEquals("fake-order-id", result.orderId)
        assertTrue(result.didAttemptThreeDSecureAuthentication)
        assertNull(result.status)
    }

    @Test
    fun `completeAuthRequest() returns vault success`() {
        sut = CardAuthLauncher(browserSwitchClient)

        val scheme = "com.paypal.android.demo"
        val domain = "example.com"
        val successDeepLink = "$scheme://$domain/success"
        val options = BrowserSwitchOptions(
            targetUri = "https://fake.com/destination".toUri(),
            requestCode = BrowserSwitchRequestCodes.CARD_VAULT,
            returnUrlScheme = scheme,
            appLinkUrl = null,
            metadata = vaultMetadata
        )
        val authState = BrowserSwitchPendingState(options).toBase64EncodedJSON()
        intent.data = successDeepLink.toUri()

        val result =
            sut.completeVaultAuthRequest(intent, authState) as CardFinishVaultResult.Success
        assertEquals("fake-setup-token-id", result.setupTokenId)
        assertNull(result.status)
    }
}
