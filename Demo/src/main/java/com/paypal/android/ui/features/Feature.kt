package com.paypal.android.ui.features

import androidx.annotation.StringRes
import com.paypal.android.R

enum class Feature(@StringRes val stringRes: Int) {
    CARD_APPROVE_ORDER(R.string.feature_approve_order),
    CARD_VAULT(R.string.feature_vault),
    PAYPAL_WEB(R.string.feature_paypal_web),
    PAYPAL_BUTTONS(R.string.feature_paypal_buttons),
    PAYPAL_NATIVE(R.string.feature_paypal_native)
}
