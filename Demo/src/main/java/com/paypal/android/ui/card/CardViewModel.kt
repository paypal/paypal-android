package com.paypal.android.ui.card

import androidx.lifecycle.ViewModel
import com.paypal.android.data.card.PrefillCardData

class CardViewModel : ViewModel() {
    val autoFillCards = PrefillCardData.cards
}
