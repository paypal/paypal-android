package com.paypal.android.cardpayments

import android.os.Parcelable
import com.paypal.android.cardpayments.threedsecure.SCA
import kotlinx.parcelize.Parcelize

/**
 * A card request to process a card payment.
 *
 * @property orderId The order ID to to process the payment
 * @property card   Card used for payment
 * @property returnUrl Url to return to app after SCA challenge finishes
 * @property sca Specify to always launch 3DS or only when required. Defaults to `SCA.SCA_WHEN_REQUIRED`.
 */
@Parcelize
data class CardRequest @JvmOverloads constructor(
    val orderId: String,
    val card: Card,
    val returnUrl: String,
    val sca: SCA = SCA.SCA_WHEN_REQUIRED,
) : Parcelable
