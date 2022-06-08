package com.paypal.android.ui.testcards

import com.paypal.android.data.card.TestCard

sealed class TestCardsItem {
    class Header(val title: String): TestCardsItem()
    class Data(val testCard: TestCard): TestCardsItem()
}
