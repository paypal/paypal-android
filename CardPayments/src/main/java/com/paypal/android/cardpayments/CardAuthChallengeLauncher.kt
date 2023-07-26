package com.paypal.android.cardpayments

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions

class CardAuthChallengeLauncher internal constructor(
    private val browserSwitchClient: BrowserSwitchClient
) {
    constructor() : this(BrowserSwitchClient())

    fun getResult(activity: FragmentActivity): CardAuthChallengeResult2? {
        return null
//        val browserSwitchResult = browserSwitchClient.deliverResult(activity)
//        if (browserSwitchResult != null) {
//            when (browserSwitchResult.status) {
//                BrowserSwitchStatus.SUCCESS -> handleBrowserSwitchSuccess(browserSwitchResult)
//                BrowserSwitchStatus.CANCELED -> notifyApproveOrderCanceled()
//            }
//        }
//        return null
    }

    fun launch(activity: FragmentActivity, authChallenge: CardAuthChallenge) {
        val urlScheme = authChallenge.returnUrl.scheme
        val approveOrderMetadata = authChallenge.approveOrderMetadata

        val options = BrowserSwitchOptions()
            .url(authChallenge.payerActionUri)
            .returnUrlScheme(urlScheme)
            .metadata(approveOrderMetadata.toJSON())

        browserSwitchClient.start(activity, options)
    }
}