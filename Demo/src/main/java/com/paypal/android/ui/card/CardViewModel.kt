package com.paypal.android.ui.card

import android.content.DialogInterface
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Amount
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Payee
import com.paypal.android.api.model.PurchaseUnit
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.data.card.PrefillCardData
import com.paypal.android.ui.card.validation.CardFormatter
import com.paypal.android.ui.card.validation.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardViewModel @Inject constructor (
    private val payPalDemoApi: PayPalDemoApi
) : ViewModel() {

    companion object {
        private val TAG = CardViewModel::class.qualifiedName
        private const val ORDER_ID = "45413380SJ116311E"
        private const val CLIENT_ID = "ASUApeBhpz9-IhrBRpHbBfVBklK4XOr1lvZdgu1UlSK0OvoJut6R-zPUP7iufxso55Yvyl6IZYV3yr0g"
    }

    private val _cardNumber = MutableLiveData("")
    val cardNumber: LiveData<String> = _cardNumber

    private val _expirationDate = MutableLiveData("")
    val expirationDate: LiveData<String> = _expirationDate

    private val _securityCode = MutableLiveData("")
    val securityCode: LiveData<String> = _securityCode

    var environment: String? = null
    val autoFillCards = PrefillCardData.cards

    private val configuration = PaymentsConfiguration(CLIENT_ID, Environment.SANDBOX, AUTH_TOKEN)
    private val cardClient = CardClient(configuration)

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
        val card = Card("4032036247327321", "2024-11")
        cardClient.approveOrder(ORDER_ID, card) { result ->
            if (result.response != null) {
                Log.e("DemoActivity", "SUCCESS")
                Log.e("DemoActivity", "${result.response!!.lastDigits}")
            } else {
                Log.e("DemoActivity", "ERRRORRRR")
            }
        }
    }

    private fun fetchOrderId() {
        viewModelScope.launch {
            val order = payPalDemoApi.fetchOrderId(
                countryCode = "CO",
                orderRequest = CreateOrderRequest(
                    intent = "CAPTURE",
                    purchaseUnit = listOf(
                        PurchaseUnit(
                            amount = Amount(
                                currencyCode = "USD",
                                value = "10.99"
                            )
                        )
                    ),
                    payee = Payee(
                        emailAddress = "anpelaez@paypal.com"
                    )
                )
            )
            Log.d(TAG, "$order")
        }
    }

    fun onPrefillCardSelected(cardName: String) {
        autoFillCards.find { it.first == cardName }?.second?.apply {
            _cardNumber.value = CardFormatter.formatCardNumber(cardNumber)
            _expirationDate.value = expirationDate
            _securityCode.value = securityCode
        }
    }
}
