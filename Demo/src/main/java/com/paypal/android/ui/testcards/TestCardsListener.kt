package com.paypal.android.ui.testcards

import com.paypal.android.data.card.TestCard

interface TestCardsListener {

    fun onTestCardSelected(testCard: TestCard)
}