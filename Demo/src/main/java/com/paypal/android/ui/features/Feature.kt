package com.paypal.android.ui.features

import androidx.annotation.StringRes
import com.paypal.android.R
import com.paypal.android.ui.DemoAppDestinations

enum class Feature(@StringRes val stringRes: Int, val routeName: String) {
    CARD_APPROVE_ORDER(R.string.feature_approve_order, DemoAppDestinations.CARD_APPROVE_ORDER),
    CARD_VAULT(R.string.feature_vault, DemoAppDestinations.CARD_VAULT),
    PAYPAL_WEB(R.string.feature_paypal_web, DemoAppDestinations.PAYPAL_WEB),
    PAYPAL_WEB_VAULT(R.string.feature_paypal_web_vault, DemoAppDestinations.PAYPAL_WEB_VAULT),
    PAYPAL_BUTTONS(R.string.feature_paypal_buttons, DemoAppDestinations.PAYPAL_BUTTONS),
    PAYPAL_STATIC_BUTTONS(
        R.string.feature_paypal_static_buttons,
        DemoAppDestinations.PAYPAL_STATIC_BUTTONS
    ),
}
