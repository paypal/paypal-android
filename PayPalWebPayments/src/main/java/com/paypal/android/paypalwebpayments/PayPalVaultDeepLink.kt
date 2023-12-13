package com.paypal.android.paypalwebpayments

import android.net.Uri
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient.Companion.DEEP_LINK_PARAM_APPROVAL_SESSION_ID
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient.Companion.DEEP_LINK_PARAM_APPROVAL_TOKEN_ID

internal class PayPalVaultDeepLink constructor(uri: Uri) {
    val approvalTokenId: String? = uri.getQueryParameter(DEEP_LINK_PARAM_APPROVAL_TOKEN_ID)
    val approvalSessionId: String? = uri.getQueryParameter(DEEP_LINK_PARAM_APPROVAL_SESSION_ID)

    val isValid = !approvalTokenId.isNullOrBlank() && !approvalSessionId.isNullOrBlank()
}
