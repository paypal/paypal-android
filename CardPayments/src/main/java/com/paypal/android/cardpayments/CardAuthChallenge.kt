package com.paypal.android.cardpayments

import android.net.Uri

data class CardAuthChallenge(
    internal val payerActionUri: Uri,
    internal val returnUrl: Uri,
    internal val approveOrderMetadata: ApproveOrderMetadata
)
