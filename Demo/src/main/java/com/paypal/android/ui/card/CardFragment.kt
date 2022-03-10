package com.paypal.android.ui.card

import android.os.Bundle
import android.text.Editable
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
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.databinding.FragmentCardBinding
import com.paypal.android.text.ValueChangeWatcher
import com.paypal.android.text.onValueChange
import com.paypal.android.ui.card.validation.CardFormatter
import com.paypal.android.utils.SharedPreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    companion object {
        private val TAG = CardFragment::class.qualifiedName
    }

    @Inject
    lateinit var preferenceUtil: SharedPreferenceUtil

    @Inject
    lateinit var payPalDemoApi: PayPalDemoApi

    private lateinit var binding: FragmentCardBinding

    private val configuration =
        CoreConfig(BuildConfig.CLIENT_ID, clientSecret = BuildConfig.CLIENT_SECRET)
    private val cardClient = CardClient(configuration)

    private val cardViewModel: CardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cardViewModel.environment = preferenceUtil.getEnvironment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCardBinding.inflate(inflater, container, false)

        binding.run {
            autoCompleteTextView.setAdapter(createPrefillCardsAdapter())

            autoCompleteTextView.onValueChange = ::onPrefillCardValueChange
            cardNumberInput.onValueChange = ::onCardNumberValueChange
            cardExpirationInput.onValueChange = ::onCardExpirationValueChange

            submitButton.setOnClickListener { onCardFieldSubmit() }
        }

        return binding.root
//        return ComposeView(requireContext()).apply {
//            setContent {
//                DemoTheme {
//                    Column {
//                        DropDown(
//                            cardViewModel.autoFillCards.map { it.first },
//                            stringResource(R.string.card_field_prefill_card_fields),
//                            { selectedCard -> cardViewModel.onPrefillCardSelected(selectedCard) },
//                            Modifier.padding(16.dp)
//                        )
//                        CardFields(
//                            cardViewModel,
//                            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
//                        )
//                        Button(
//                            onClick = { cardViewModel.onCardFieldSubmit() },
//                            modifier = Modifier
//                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
//                                .fillMaxWidth()
//                        ) { Text(stringResource(R.string.card_field_submit)) }
//                    }
//                }
//            }
//        }
    }

    private fun onPrefillCardValueChange(oldValue: String, newValue: String) {

    }

    private fun onCardNumberValueChange(oldValue: String, newValue: String) {
        val formattedCardNumber = CardFormatter.formatCardNumber(newValue, oldValue)
        binding.cardNumberInput.setText(formattedCardNumber)
        binding.cardNumberInput.setSelection(formattedCardNumber.length)
    }

    private fun onCardExpirationValueChange(oldValue: String, newValue: String) {

    }

    private fun createPrefillCardsAdapter(): ArrayAdapter<String> {
        val autoFillCardNames = cardViewModel.autoFillCards.map { it.first }
        return ArrayAdapter(requireActivity(), R.layout.dropdown_item, autoFillCardNames)
    }

    private fun initializeDropDown() {
        // set drop down menu items
        val autoFillCardNames = cardViewModel.autoFillCards.map { it.first }
        val adapter = ArrayAdapter(requireActivity(), R.layout.dropdown_item, autoFillCardNames)

        binding.run {
            autoCompleteTextView.setAdapter(adapter)
//            autoCompleteTextView.onValueChange = {
//                cardViewModel.selectedPrefillCard.value = autoCompleteTextView.text.toString()
//            }
        }

        cardViewModel.selectedPrefillCard.observe(viewLifecycleOwner) { cardName ->
            cardViewModel.run {
                autoFillCards.find { it.first == cardName }?.second?.apply {

//                    _cardNumber.value = CardFormatter.formatCardNumber(number)
//                    _expirationDate.value = "$expirationMonth/$expirationYear"
//                    _securityCode.value = securityCode
                }
            }
        }
    }

    private fun onCardFieldSubmit() {
        val cardNumber = CardFormatter.formatCardNumber(binding.cardNumberInput.text.toString())
        val expirationDate = binding.cardExpirationInput.text.toString()
        val securityCode = binding.cardSecurityCodeInput.text.toString()

        Log.d(TAG, "$cardNumber")
        Log.d(TAG, "$expirationDate")
        Log.d(TAG, "$securityCode")
        Log.d(TAG, "Environment = ${cardViewModel.environment}")

        val (monthString, yearString) =
            expirationDate.split("/") ?: listOf("", "")

        val card = Card(cardNumber, monthString, yearString)
        card.securityCode = securityCode

        lifecycleScope.launch {
            val order = fetchOrder()
            val request = CardRequest(order.id!!, card)

            try {
                val result = cardClient.approveOrder(request)
                Log.d(TAG, "SUCCESS")
                Log.d(TAG, "${result.status}")
            } catch (error: PayPalSDKError) {
                Log.e(TAG, "ERROR")
                Log.e(TAG, error.errorDescription.orEmpty())
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
}
