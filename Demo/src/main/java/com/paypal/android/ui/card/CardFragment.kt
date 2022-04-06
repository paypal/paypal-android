package com.paypal.android.ui.card

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.paypal.android.BuildConfig
import com.paypal.android.R
import com.paypal.android.api.model.*
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.card.Card
import com.paypal.android.card.CardClient
import com.paypal.android.card.CardRequest
import com.paypal.android.core.Address
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.databinding.FragmentCardBinding
import com.paypal.android.text.onValueChange
import com.paypal.android.ui.card.validation.CardFormatter
import com.paypal.android.ui.card.validation.DateFormatter
import com.paypal.android.utils.SharedPreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    @Inject
    lateinit var preferenceUtil: SharedPreferenceUtil

    @Inject
    lateinit var payPalDemoApi: PayPalDemoApi

    @Inject
    lateinit var dataCollectorHandler: DataCollectorHandler

    private lateinit var binding: FragmentCardBinding

    private val configuration = CoreConfig(BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET)
    private val cardClient = CardClient(configuration)

    private val cardViewModel: CardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCardBinding.inflate(inflater, container, false)

        binding.run {
            autoCompleteTextView.setAdapter(createPrefillCardsAdapter())

            autoCompleteTextView.onValueChange = ::onPrefillCardChange
            cardNumberInput.onValueChange = ::onCardNumberChange
            cardExpirationInput.onValueChange = ::onCardExpirationDateChange

            submitButton.setOnClickListener { onCardFieldSubmit() }
        }
        return binding.root
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPrefillCardChange(oldValue: String, newValue: String) {
        val autoFillCards = cardViewModel.autoFillCards
        autoFillCards[newValue]?.let { autoFillCard(it) }
    }

    private fun autoFillCard(card: Card) {
        binding.run {
            card.run {
                val previousCardNumber = cardNumberInput.text.toString()
                val formattedCardNumber = CardFormatter.formatCardNumber(number, previousCardNumber)
                cardNumberInput.setText(formattedCardNumber)

                val expirationDate = "$expirationMonth/$expirationYear"
                cardExpirationInput.setText(expirationDate)
                cardSecurityCodeInput.setText(securityCode)
            }
        }
    }

    private fun onCardNumberChange(oldValue: String, newValue: String) {
        val formattedCardNumber = CardFormatter.formatCardNumber(newValue, oldValue)
        binding.cardNumberInput.setText(formattedCardNumber)
        binding.cardNumberInput.setSelection(formattedCardNumber.length)
    }

    private fun onCardExpirationDateChange(oldValue: String, newValue: String) {
        val formattedExpirationDate = DateFormatter.formatExpirationDate(newValue, oldValue)
        binding.cardExpirationInput.setText(formattedExpirationDate)
        binding.cardExpirationInput.setSelection(formattedExpirationDate.length)
    }

    private fun createPrefillCardsAdapter(): ArrayAdapter<String> {
        val autoFillCardNames = cardViewModel.autoFillCards.keys.toList()
        return ArrayAdapter(requireActivity(), R.layout.dropdown_item, autoFillCardNames)
    }

    private fun onCardFieldSubmit() {
        val cardNumber = binding.cardNumberInput.text.toString()
        val expirationDate = binding.cardExpirationInput.text.toString()
        val securityCode = binding.cardSecurityCodeInput.text.toString()

        val (monthString, yearString) =
            expirationDate.split("/") ?: listOf("", "")

        val card = Card(cardNumber, monthString, yearString)
        card.securityCode = securityCode

        dataCollectorHandler.setLogging(true)
        lifecycleScope.launch {
            updateStatusText("Creating order...")
            val order = fetchOrder(true)
            val request = CardRequest(order.id!!, card)
            val clientMetadataId = dataCollectorHandler.getClientMetadataId(order.id)
            Log.i("Magnes", "MetadataId: $clientMetadataId")
            updateStatusText("Authorizing order...")
            try {
                cardClient.verifyCard(requireActivity(), order.id, card)
                updateStatusText("CAPTURE success: CONFIRMED")
            } catch (error: PayPalSDKError) {
                updateStatusText("CAPTURE fail: ${error.errorDescription}")
            }
        }
    }

    private suspend fun fetchOrder(verifyUsing3DS: Boolean = false): Order {
        val orderRequest = CreateOrderRequest(
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

        if (verifyUsing3DS) {
            orderRequest.applicationContext = ApplicationContext(
                returnURL = "com.paypal.android.demo://example.com/returnUrl",
                cancelURL = "com.paypal.android.demo://example.com/cancelUrl"
            )
        }
        return payPalDemoApi.fetchOrderId(countryCode = "CO", orderRequest = orderRequest)
    }

    private fun updateStatusText(text: String) {
        binding.statusText.text = text
    }
}
