package com.paypal.android.threedsecure

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.card.CardRequest
import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig

class ThreeDSecureClient internal constructor(private val activity: FragmentActivity, private val threeDSecureAPI: ThreeDSecureAPI) {

    private val browserSwitchClient = BrowserSwitchClient()

    constructor(activity: FragmentActivity, configuration: CoreConfig) : this(activity, ThreeDSecureAPI(API(configuration)))

    init {
        activity.lifecycle.addObserver(ThreeDSecureLifeCycleObserver(this))
    }

    suspend fun verify(cardRequest: CardRequest) {
        //val result = cardRequest.run { threeDSecureAPI.verifyCard(orderID, card) }

        // FUTURE: inspect URL for 3DS verification success / failure
//        val options = BrowserSwitchOptions()
//            .url(Uri.parse(result.payerActionHref))
//            .returnUrlScheme("com.paypal.android.demo")
//        browserSwitchClient.start(activity, options)
    }

    internal fun handleBrowserSwitchResult() {
        val result = browserSwitchClient.deliverResult(activity)
    }
}
