package com.paypal.android.models

import android.os.Parcelable
import com.paypal.android.cardpayments.Card
import com.paypal.android.ui.card.validation.CardFormatter
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class TestCard(val name: String, val card: Card) : Parcelable {

    @IgnoredOnParcel
    val formattedCardNumber: String = CardFormatter.formatCardNumber(card.number)
}
