package com.paypal.android.ui.prefillcards

import com.paypal.android.data.card.TestCard

interface PrefillCardsListener {

    fun onPrefillCardSelected(testCard: TestCard)
}