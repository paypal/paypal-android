package com.paypal.android.ui.card

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.BuildConfig
import com.paypal.android.api.model.Amount
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.Payee
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.PurchaseUnit
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.card.Card
import com.paypal.android.card.CardClient
import com.paypal.android.core.CoreConfig
import com.paypal.android.data.card.PrefillCardData
import com.paypal.android.ui.card.validation.CardFormatter
import com.paypal.android.ui.card.validation.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardViewModel @Inject constructor(
    private val payPalDemoApi: PayPalDemoApi
) : ViewModel() {

    companion object {
        private val TAG = CardViewModel::class.qualifiedName
    }

    private val _cardNumber = MutableLiveData("")
    val cardNumber: LiveData<String> = _cardNumber

    private val _expirationDate = MutableLiveData("")
    val expirationDate: LiveData<String> = _expirationDate

    private val _securityCode = MutableLiveData("")
    val securityCode: LiveData<String> = _securityCode

    var environment: String? = null
    val autoFillCards = PrefillCardData.cards

    private val configuration =
        CoreConfig(BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET)
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

        val number = _cardNumber.value ?: ""
        val (monthString, yearString) =
            _expirationDate.value?.split("/") ?: listOf("", "")

        val card = Card(number, monthString, yearString)
        card.securityCode = _securityCode.value ?: ""

        viewModelScope.launch {
            val order = fetchOrder()
            cardClient.approveOrder(order.id!!, card) { result ->
                result.response?.let { response ->
                    Log.d(TAG, "SUCCESS")
                    Log.d(TAG, "${response.status}")
                }

                result.error?.let { error ->
                    Log.e(TAG, "ERRRORRRR")
                    error.message?.let { Log.e(TAG, it) }
                }
            }
        }
    }

    private suspend fun fetchOrder(): Order {
        return payPalDemoApi.fetchOrderId(
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
    }

    fun onPrefillCardSelected(cardName: String) {
        autoFillCards.find { it.first == cardName }?.second?.apply {
            _cardNumber.value = CardFormatter.formatCardNumber(number)
            _expirationDate.value = "$expirationMonth/$expirationYear"
            _securityCode.value = securityCode
        }
    }
}
