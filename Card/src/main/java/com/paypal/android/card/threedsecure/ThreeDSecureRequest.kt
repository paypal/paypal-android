package com.paypal.android.card.threedsecure

data class ThreeDSecureRequest(val sca: SCA = SCA.SCA_WHEN_REQUIRED, val returnUrl: String, val cancelUrl: String)
