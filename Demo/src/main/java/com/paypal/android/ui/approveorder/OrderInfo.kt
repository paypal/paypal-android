package com.paypal.android.ui.approveorder

data class OrderInfo(
    val orderId: String,
    val status: String?,
    val didAttemptThreeDSecureAuthentication: Boolean
)
