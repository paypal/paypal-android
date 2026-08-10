package com.paypal.android.corepayments.browserswitch

import androidx.browser.customtabs.CustomTabsSession

internal data class PreparedCustomTab(
    val session: CustomTabsSession,
    val binding: SessionBinding,
    val packageName: String
)
