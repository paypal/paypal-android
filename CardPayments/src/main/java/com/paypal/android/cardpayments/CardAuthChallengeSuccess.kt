package com.paypal.android.cardpayments

import android.net.Uri

internal data class CardAuthChallengeSuccess(
    internal val approveOrderMetadata: ApproveOrderMetadata,
    internal val deepLinkUrl: Uri? = null
) : CardAuthChallengeResult2
