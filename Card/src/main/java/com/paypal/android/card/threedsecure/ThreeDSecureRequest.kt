package com.paypal.android.card.threedsecure

data class ThreeDSecureRequest(val sca: SCA, val returnUrl: String, val cancelUrl: String)
