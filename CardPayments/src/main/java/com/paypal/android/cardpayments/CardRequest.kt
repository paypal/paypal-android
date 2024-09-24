package com.paypal.android.cardpayments

import android.os.Parcelable
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.CoreConfig
import kotlinx.parcelize.Parcelize

sealed class CardRequest {

    /**
     * A card request to process a card payment.
     *
     * @property orderId The order ID to to process the payment
     * @property card   Card used for payment
     * @property returnUrl Url to return to app after SCA challenge finishes
     * @property sca Specify to always launch 3DS or only when required. Defaults to `SCA.SCA_WHEN_REQUIRED`.
     */
    @Parcelize
    data class ApproveOrder(
        val config: CoreConfig,
        val orderId: String,
        val card: Card,
        val returnUrl: String,
        val sca: SCA = SCA.SCA_WHEN_REQUIRED,
    ) : Parcelable, CardRequest()

    /**
     * @suppress
     *
     * A vault request to attach a payment method to a setup token.
     *
     * @property setupTokenId id for the setup token to update.
     * @property card card payment source to attach to the setup token.
     * @property returnUrl return url for deep linking back into the merchant app after an auth challenge
     */
    @Parcelize
    data class Vault(
        val config: CoreConfig,
        val setupTokenId: String,
        val card: Card,
        val returnUrl: String? = "",
    ) : Parcelable, CardRequest()

}

