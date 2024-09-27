package com.paypal.android.corepayments.browserswitch

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
enum class BrowserSwitchRequestCode: Parcelable {
    CARD_APPROVE_ORDER,
    CARD_VAULT,
    PAYPAL_CHECKOUT,
    PAYPAL_VAULT,
}