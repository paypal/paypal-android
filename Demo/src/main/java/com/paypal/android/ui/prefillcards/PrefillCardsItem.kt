package com.paypal.android.ui.prefillcards

import com.paypal.android.data.card.PrefillCard

sealed class PrefillCardsItem

class PrefillCardsHeader(val title: String): PrefillCardsItem()
class PrefillCardsData(val prefillCard: PrefillCard): PrefillCardsItem()
