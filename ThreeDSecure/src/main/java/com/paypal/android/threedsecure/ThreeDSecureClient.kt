package com.paypal.android.threedsecure

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.card.CardRequest
import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig

class ThreeDSecureClient internal constructor(private val threeDSecureAPI: ThreeDSecureAPI) {

    private val browserSwitchClient = BrowserSwitchClient()

    constructor(configuration: CoreConfig) : this(ThreeDSecureAPI(API(configuration)))

    suspend fun verify(activity: FragmentActivity, cardRequest: CardRequest) {
        val result = cardRequest.run { threeDSecureAPI.verifyCard(orderID, card) }

        // FUTURE: inspect URL for 3DS verification success / failure
        val options = BrowserSwitchOptions()
            .url(Uri.parse(result.payerActionHref))
            .returnUrlScheme("com.paypal.android.demo")
        browserSwitchClient.start(activity, options)
    }
}
