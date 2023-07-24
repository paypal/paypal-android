package com.paypal.android.ui.features

import android.os.Parcelable
import androidx.annotation.StringRes
import com.paypal.android.R
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Feature(@StringRes val stringRes: Int) : Parcelable {
    CARD_APPROVE_ORDER(R.string.feature_approve_order),
    CARD_VAULT(R.string.feature_vault),
    PAYPAL_WEB(R.string.feature_paypal_web),
    PAYPAL_NATIVE(R.string.feature_paypal_native)
}
