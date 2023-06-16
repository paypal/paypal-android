package com.paypal.android.data.card

import android.os.Parcelable
import com.paypal.android.cardpayments.Card
import kotlinx.parcelize.Parcelize

@Parcelize
data class TestCard(val name: String, val card: Card) : Parcelable
