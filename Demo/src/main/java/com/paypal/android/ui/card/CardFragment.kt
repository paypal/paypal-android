package com.paypal.android.ui.card

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.paypal.android.R
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Payee
import com.paypal.android.api.services.SDKSampleServerApi
import com.paypal.android.card.ApproveOrderListener
import com.paypal.android.card.Card
import com.paypal.android.card.CardClient
import com.paypal.android.card.CardRequest
import com.paypal.android.card.model.CardResult
import com.paypal.android.card.threedsecure.SCA
import com.paypal.android.corepayments.Address
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.databinding.FragmentCardBinding
import com.paypal.android.text.onValueChange
import com.paypal.android.ui.card.validation.CardFormatter
import com.paypal.android.ui.card.validation.DateFormatter
import com.paypal.android.ui.testcards.TestCardsFragment
import com.paypal.android.utils.SharedPreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    companion object {
        const val TAG = "CardFragment"
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
    }

    @Inject
    lateinit var preferenceUtil: SharedPreferenceUtil

    @Inject
    lateinit var sdkSampleServerApi: SDKSampleServerApi

    @Inject
    lateinit var dataCollectorHandler: DataCollectorHandler

    private lateinit var cardClient: CardClient
    private lateinit var binding: FragmentCardBinding

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
            cardNumberInput.onValueChange = ::onCardNumberChange
            cardExpirationInput.onValueChange = ::onCardExpirationDateChange

            useTestCardButton.setOnClickListener { showTestCards() }
            submitButton.setOnClickListener { onCardFieldSubmit() }
        }

        setFragmentResultListener(TestCardsFragment.REQUEST_KEY) { _, bundle ->
            handleTestCardSelected(bundle)
        }
    }

    private fun handleTestCardSelected(bundle: Bundle) {
        val cardNumber = bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_NUMBER)
        val securityCode = bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_SECURITY_CODE)

        val expirationMonth =
            bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_EXPIRATION_MONTH)
        val expirationYear =
            bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_EXPIRATION_YEAR)

        binding.run {
            cardNumberInput.setText("")
            val formattedCardNumber =
                CardFormatter.formatCardNumber(cardNumber ?: "")
            cardNumberInput.setText(formattedCardNumber)

            val expirationDate = "$expirationMonth/$expirationYear"
            cardExpirationInput.setText(expirationDate)
            cardSecurityCodeInput.setText(securityCode)
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

    private fun showTestCards() {
        findNavController().navigate(R.id.action_cardFragment_to_testCardFragment)
    }

    private suspend fun createOrder() {

        val accessToken = sdkSampleServerApi.fetchAccessToken().value
        val configuration = CoreConfig(accessToken = accessToken)
        cardClient = CardClient(requireActivity(), configuration)

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

        dataCollectorHandler.setLogging(true)
        updateStatusText("Creating order...")

        val orderRequest = buildOrderRequest()
        val order = sdkSampleServerApi.createOrder(orderRequest = orderRequest)

        val clientMetadataId = dataCollectorHandler.getClientMetadataId(order.id)
        Log.i(TAG, "MetadataId: $clientMetadataId")

        updateStatusText("Authorizing order...")

        // build card request
        val cardRequest = binding.run {
            val cardNumber = cardNumberInput.text.toString().replace(" ", "")
            val expirationDate = cardExpirationInput.text.toString()
            val securityCode = cardSecurityCodeInput.text.toString()

            val (monthString, yearString) = expirationDate.split("/")

            val billingAddress = Address(
                countryCode = "US",
                streetAddress = "3272 Gateway Road",
                locality = "Aloha",
                postalCode = "97007"
            )

            val card = Card(cardNumber, monthString, yearString, securityCode, billingAddress = billingAddress)
            val sca = when (radioGroup3DS.checkedRadioButtonId) {
                R.id.sca_when_required -> SCA.SCA_WHEN_REQUIRED
                else -> SCA.SCA_ALWAYS
            }
            CardRequest(
                order.id!!,
                card,
                APP_RETURN_URL,
                sca
            )
        }

        cardClient.approveOrder(requireActivity(), cardRequest)
    }

    private fun buildOrderRequest(): CreateOrderRequest = CreateOrderRequest(
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

    private fun updateStatusText(text: String) {
        if (!isDetached) {
            requireActivity().runOnUiThread {
                binding.statusText.text = text
            }
        }
    }
}
