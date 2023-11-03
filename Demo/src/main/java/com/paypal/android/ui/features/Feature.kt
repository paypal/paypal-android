package com.paypal.android.ui.features

import androidx.annotation.StringRes
import com.paypal.android.R

enum class Feature(@StringRes val stringRes: Int, val routeName: String) {
    CARD_APPROVE_ORDER(R.string.feature_approve_order, "cardApproveOrder"),
    CARD_VAULT(R.string.feature_vault, "cardVault"),
    PAYPAL_WEB(R.string.feature_paypal_web, "payPalWeb"),
    PAYPAL_BUTTONS(R.string.feature_paypal_buttons, "payPalButtons"),
    PAYPAL_NATIVE(R.string.feature_paypal_native, "payPalNative")
}

