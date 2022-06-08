package com.paypal.android.ui.prefillcards

import com.paypal.android.data.card.TestCard

sealed class PrefillCardsItem {
    class Header(val title: String): PrefillCardsItem()
    class Data(val testCard: TestCard): PrefillCardsItem()
}
