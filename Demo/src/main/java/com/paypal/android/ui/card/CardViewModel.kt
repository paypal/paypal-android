package com.paypal.android.ui.card

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class CardViewModel : ViewModel() {
    private val _focusedOption = MutableStateFlow<CardOption?>(null)

    val scaOptionExpanded = _focusedOption.map { it == CardOption.SCA }
    val intentOptionExpanded = _focusedOption.map { it == CardOption.INTENT }
    val shouldVaultOptionExpanded = _focusedOption.map { it == CardOption.SHOULD_VAULT }

    fun onOptionChange(option: CardOption, value: String) {

    }

    fun onFocusChange(option: CardOption) {
        _focusedOption.value = option
    }

    fun clearFocus() {
        _focusedOption.value = null
    }
}