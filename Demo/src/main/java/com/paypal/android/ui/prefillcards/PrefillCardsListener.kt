package com.paypal.android.ui.prefillcards

import com.paypal.android.data.card.PrefillCard

interface PrefillCardsListener {

    fun onPrefillCardSelected(prefillCard: PrefillCard)
}