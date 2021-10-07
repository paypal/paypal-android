package com.paypal.android.card

enum class CardVerificationMethod(val asString: String) {
    THREED_SECURE("3D_SECURE"), AVS_CVV("AVS_CVV")
}