package com.paypal.android.ui.prefillcards

import com.paypal.android.data.card.PrefillCard

sealed class PrefillCardsItem {
    class Header(val title: String): PrefillCardsItem()
    class Data(val prefillCard: PrefillCard): PrefillCardsItem()
}
