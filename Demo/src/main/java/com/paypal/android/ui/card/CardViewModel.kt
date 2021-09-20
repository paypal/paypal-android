package com.paypal.android.ui.card

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CardViewModel : ViewModel() {

    private val _cardNumber = MutableLiveData("")
    val cardNumber: LiveData<String> = _cardNumber

    private val _expirationDate = MutableLiveData("")
    val expirationDate: LiveData<String> = _expirationDate

    private val _securityCode = MutableLiveData("")
    val securityCode: LiveData<String> = _securityCode

    fun onCardNumberChange(newCardNumber: String) {
        _cardNumber.value = newCardNumber
    }

    fun onExpirationDateChange(newExpirationDate: String) {
        _expirationDate.value = formatExpirationDate(newExpirationDate)
    }

    fun onSecurityCodeChange(newSecurityCode: String) {
        _securityCode.value = newSecurityCode
    }

    fun onCardFieldSubmit() {
        Log.d(TAG, "${cardNumber.value}")
        Log.d(TAG, "${expirationDate.value}")
        Log.d(TAG, "${securityCode.value}")

        // Invoke Card SDK
    }

    private fun formatExpirationDate(expirationDate: String): String {
        return if (expirationDate.length == 2) "$expirationDate/" else expirationDate
    }

    companion object {
        private val TAG = CardViewModel::class.qualifiedName
    }
}
