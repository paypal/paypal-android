package com.paypal.android.card

import com.paypal.android.card.threedsecure.SCA

/**
 * A card request to process a card payment
 * @property orderID The order ID to to process the payment
 * @property card   Card used for payment
 * @property returnUrl Url to return to app after SCA challenge finishes
 * @property sca Specify to always launch 3DS or only when required. Defaults to `SCA.SCA_WHEN_REQUIRED`.
 */
data class CardRequest @JvmOverloads constructor(
    val orderID: String,
    val card: Card,
    val returnUrl: String,
    val sca: SCA = SCA.SCA_WHEN_REQUIRED
)
