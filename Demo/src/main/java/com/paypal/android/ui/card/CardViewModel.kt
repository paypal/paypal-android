package com.paypal.android.ui.card

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.paypal.android.data.card.PrefillCardData
import com.paypal.android.ui.card.validation.CardFormatter
import com.paypal.android.ui.card.validation.DateFormatter

class CardViewModel : ViewModel() {

    private val _cardNumber = MutableLiveData("")
    val cardNumber: LiveData<String> = _cardNumber

    private val _expirationDate = MutableLiveData("")
    val expirationDate: LiveData<String> = _expirationDate

    private val _securityCode = MutableLiveData("")
    val securityCode: LiveData<String> = _securityCode

    var environment: String? = null
    val autoFillCards = PrefillCardData.cards

    fun onCardNumberChange(newCardNumber: String) {
        _cardNumber.value = CardFormatter.formatCardNumber(newCardNumber, _cardNumber.value ?: "")
    }

    fun onExpirationDateChange(newExpirationDate: String) {
        _expirationDate.value = DateFormatter.formatExpirationDate(
            newDateString = newExpirationDate,
            previousDateString = _expirationDate.value
        )
    }

    fun onSecurityCodeChange(newSecurityCode: String) {
        _securityCode.value = newSecurityCode
    }

    fun onCardFieldSubmit() {
        Log.d(TAG, "${cardNumber.value}")
        Log.d(TAG, "${expirationDate.value}")
        Log.d(TAG, "${securityCode.value}")
        Log.d(TAG, "Environment = $environment")

        // Invoke Card SDK
    }

    fun onPrefillCardSelected(cardName: String) {
        autoFillCards.find { it.first == cardName }?.second?.apply {
            _cardNumber.value = CardFormatter.formatCardNumber(cardNumber)
            _expirationDate.value = expirationDate
            _securityCode.value = securityCode
        }
    }

    companion object {
        private val TAG = CardViewModel::class.qualifiedName
    }
}
