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
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.Payee
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.card.model.Amount
import com.paypal.android.card.ApproveOrderListener
import com.paypal.android.card.Card
import com.paypal.android.card.CardClient
import com.paypal.android.card.CardRequest
import com.paypal.android.card.model.CardResult
import com.paypal.android.card.OrderIntent
import com.paypal.android.card.OrderRequest
import com.paypal.android.card.model.PurchaseUnit
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID
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
    private lateinit var cardClient: CardClient

    private val cardViewModel: CardViewModel by viewModels()

    private var job = Job()

    private val orderRequest = OrderRequest(
        OrderIntent.CAPTURE, listOf(
            PurchaseUnit(
                referenceId = UUID.randomUUID().toString(),
                amount = Amount(
                    currencyCode = "USD",
                    value = "10.99"
                )
            ),
            PurchaseUnit(
                referenceId = UUID.randomUUID().toString(),
                amount = Amount(
                    currencyCode = "USD",
                    value = "15.00"
                )
            )
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        cardClient = CardClient(requireActivity(), configuration)
        binding = FragmentCardBinding.inflate(inflater, container, false)

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

    private fun onCardFieldSubmit() {
        val cardNumber = binding.cardNumberInput.text.toString()
        val expirationDate = binding.cardExpirationInput.text.toString()
        val securityCode = binding.cardSecurityCodeInput.text.toString()
        val isThreeDSecure = binding.threedsChkbox.isChecked

        val (monthString, yearString) =
            expirationDate.split("/") ?: listOf("", "")

        val card = Card(cardNumber, monthString, yearString)
        card.securityCode = securityCode

        val threeDSecureRequest = if (isThreeDSecure) {
            ThreeDSecureRequest(
                sca = SCA.SCA_ALWAYS,
                returnUrl = "com.paypal.android.demo://example.com/returnUrl",
                cancelUrl = "com.paypal.android.demo://example.com/cancelUrl"
            )
        } else {
            null
        }

        serverSideIntegration(card, threeDSecureRequest)
    }

    private fun serverSideIntegration(card: Card, threeDSecureRequest: ThreeDSecureRequest?) {
        dataCollectorHandler.setLogging(true)
        job = Job()
        lifecycleScope.launch {
            updateStatusText("Creating order...")
            val order = fetchOrder(threeDSecureRequest)
            val cardRequest = CardRequest(card)
            val clientMetadataId = dataCollectorHandler.getClientMetadataId(order.id)
            Log.i("Magnes", "MetadataId: $clientMetadataId")
            updateStatusText("Authorizing order...")
            cardClient.approveOrderListener = object : ApproveOrderListener {
                override fun onApproveOrderSuccess(result: CardResult) {
                    val statusText =
                        "Confirmed Order: ${result.orderID}, status: ${result.status?.name}"
                    val paymentSourceText = result.paymentSource?.let {
                        val text = "\nCard -> lastDigits: ${it.lastDigits}, brand: ${it.brand}, type: ${it.type}"
                        val authText = it.authenticationResult?.let { auth ->
                            val threeDtext = "\nLiability shift: ${auth.liabilityShift}," +
                                    "Enrollment: ${auth.threeDSecure?.enrollmentStatus}," +
                                    "Authentication: ${auth.threeDSecure?.authenticationStatus}"
                            threeDtext
                        }
                        text + authText
                    }
                    updateStatusText(statusText + paymentSourceText)
                }

                override fun onApproveOrderFailure(error: PayPalSDKError) {
                    updateStatusText("CAPTURE fail: ${error.errorDescription}")
                }

                override fun onApproveOrderCanceled() {
                    updateStatusText("USER CANCELLED")
                }

                override fun onApproveOrderThreeDSecureWillLaunch() {
                    updateStatusText("3DS launched")
                }

                override fun onApproveOrderThreeDSecureDidFinish() {
                    updateStatusText("3DS finished")
                }
            }
            cardClient.approveOrder(
                orderId = order.id!!,
                cardRequest = cardRequest,
                threeDSecureRequest = threeDSecureRequest,
                coroutineContext = job + Dispatchers.IO
            )
        }
    }

    private suspend fun fetchOrder(threeDSecureRequest: ThreeDSecureRequest?): Order {
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
            payee = Payee(
                emailAddress = "anpelaez@paypal.com"
            )
        )
        threeDSecureRequest?.let {
            createOrderRequest.applicationContext = ApplicationContext(
                returnURL = "com.paypal.android.demo://example.com/returnUrl",
                cancelURL = "com.paypal.android.demo://example.com/cancelUrl"
            )
        }
        return payPalDemoApi.fetchOrderId(
            countryCode = "CO",
            orderRequest = createOrderRequest
        )
    }

    private fun updateStatusText(text: String) {
        if (!isDetached) {
            requireActivity().runOnUiThread {
                binding.statusText.text = text
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (job.isActive) job.cancel()
    }

}
