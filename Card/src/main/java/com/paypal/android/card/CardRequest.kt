package com.paypal.android.card

import com.paypal.android.card.threedsecure.ThreeDSecureRequest

data class CardRequest(
    val orderID: String,
    val card: Card,
    var threeDSecureRequest: ThreeDSecureRequest? = null
)
