package com.paypal.android.ui.testcards

import com.paypal.android.data.card.TestCard

interface PrefillCardsListener {

    fun onPrefillCardSelected(testCard: TestCard)
}