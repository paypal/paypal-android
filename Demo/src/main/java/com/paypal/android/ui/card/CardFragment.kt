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
import com.paypal.android.api.model.ApplicationContext
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Payee
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.card.ApproveOrderListener
import com.paypal.android.card.Card
import com.paypal.android.card.CardClient
import com.paypal.android.card.CardRequest
import com.paypal.android.card.model.CardResult
import com.paypal.android.card.threedsecure.SCA
import com.paypal.android.card.threedsecure.ThreeDSecureRequest
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

    companion object {
        const val TAG = "CardFragment"
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
        const val APP_CANCEL_URL = "com.paypal.android.demo://example.com/cancelUrl"
    }

    @Inject
    lateinit var preferenceUtil: SharedPreferenceUtil

    @Inject
    lateinit var payPalDemoApi: PayPalDemoApi

    @Inject
    lateinit var dataCollectorHandler: DataCollectorHandler

    private val configuration = CoreConfig(BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET)
    private val cardViewModel: CardViewModel by viewModels()

    private lateinit var binding: FragmentCardBinding

    private val cardClient by lazy {
        CardClient(requireActivity(), configuration)
    }

    private val shouldRequestThreeDSecure: Boolean
        get() = binding.threedsChkbox.isChecked

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.run {
            val autoFillCardNames = cardViewModel.autoFillCards.keys.toList()
            autoCompleteTextView.setAdapter(
                ArrayAdapter(
                    requireActivity(),
                    R.layout.dropdown_item,
                    autoFillCardNames
                )
            )

            autoCompleteTextView.onValueChange = ::onPrefillCardChange
            cardNumberInput.onValueChange = ::onCardNumberChange
            cardExpirationInput.onValueChange = ::onCardExpirationDateChange

            submitButton.setOnClickListener { onCardFieldSubmit() }
        }

        cardClient.approveOrderListener = object : ApproveOrderListener {
            override fun onApproveOrderSuccess(result: CardResult) {
                val statusText =
                    "Confirmed Order: ${result.orderID}, status: ${result.status?.name}"
                val paymentSourceText = result.paymentSource?.let {
                    val text =
                        "\nCard -> lastDigits: ${it.lastDigits}, brand: ${it.brand}, type: ${it.type}"
                    val authText = it.authenticationResult?.let { auth ->
                        val threeDtext = "\nLiability shift: ${auth.liabilityShift}," +
                                "Enrollment: ${auth.threeDSecure?.enrollmentStatus}," +
                                "Authentication: ${auth.threeDSecure?.authenticationStatus}"
                        threeDtext
                    }
                    text + authText
                } ?: ""

                val deepLink = result.deepLinkUrl?.toString().orEmpty()
                val joinedText = listOf(statusText, paymentSourceText, deepLink).joinToString("\n")
                updateStatusText(joinedText)
            }

            override fun onApproveOrderFailure(error: PayPalSDKError) {
                updateStatusText("CAPTURE fail: ${error.errorDescription}")
            }

            override fun onApproveOrderCanceled() {
                updateStatusText("USER CANCELED")
            }

            override fun onApproveOrderThreeDSecureWillLaunch() {
                updateStatusText("3DS launched")
            }

            override fun onApproveOrderThreeDSecureDidFinish() {
                updateStatusText("3DS finished")
            }
        }
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

    private fun onCardFieldSubmit() {
        viewLifecycleOwner.lifecycleScope.launch {
            createOrder()
        }
    }

    private suspend fun createOrder() {
        dataCollectorHandler.setLogging(true)
        updateStatusText("Creating order...")

        val orderRequest = buildOrderRequest()
        val order = payPalDemoApi.fetchOrderId(countryCode = "CO", orderRequest = orderRequest)

        val clientMetadataId = dataCollectorHandler.getClientMetadataId(order.id)
        Log.i(TAG, "MetadataId: $clientMetadataId")

        updateStatusText("Authorizing order...")

        // build card request
        val cardRequest = binding.run {
            val cardNumber = cardNumberInput.text.toString()
            val expirationDate = cardExpirationInput.text.toString()
            val securityCode = cardSecurityCodeInput.text.toString()

            val (monthString, yearString) = expirationDate.split("/")

            val card = Card(cardNumber, monthString, yearString, securityCode)
            CardRequest(order.id!!, card)
        }

        if (shouldRequestThreeDSecure) {
            cardRequest.threeDSecureRequest = ThreeDSecureRequest(
                sca = SCA.SCA_ALWAYS,
                returnUrl = APP_RETURN_URL,
                cancelUrl = APP_CANCEL_URL
            )
        }
        cardClient.approveOrder(requireActivity(), cardRequest)
    }

    private fun buildOrderRequest(): CreateOrderRequest {
        val createOrderRequest = CreateOrderRequest(
            intent = "AUTHORIZE",
            purchaseUnit = listOf(
                com.paypal.android.api.model.PurchaseUnit(
                    amount = com.paypal.android.api.model.Amount(
                        currencyCode = "USD",
                        value = "10.99"
                    )
                )
            ),
            payee = Payee(emailAddress = "anpelaez@paypal.com")
        )

        if (shouldRequestThreeDSecure) {
            createOrderRequest.applicationContext = ApplicationContext(
                returnURL = APP_RETURN_URL,
                cancelURL = APP_CANCEL_URL
            )
        }
        return createOrderRequest
    }

    private fun updateStatusText(text: String) {
        if (!isDetached) {
            requireActivity().runOnUiThread {
                binding.statusText.text = text
            }
        }
    }
}
