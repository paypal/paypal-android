package com.paypal.android.ui.approveorder

data class SetupTokenInfo(
    val setupTokenId: String,
    val status: String? = null,
    val didAttemptThreeDSecureAuthentication: Boolean = false
)
