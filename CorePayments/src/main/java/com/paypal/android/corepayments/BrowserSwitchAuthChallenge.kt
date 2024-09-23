package com.paypal.android.corepayments

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchOptions

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface BrowserSwitchAuthChallenge {
    val options: BrowserSwitchOptions
}