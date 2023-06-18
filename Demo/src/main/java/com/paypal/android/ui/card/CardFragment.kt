package com.paypal.android.ui.card

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.paypal.android.api.model.Amount
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Payee
import com.paypal.android.api.model.PurchaseUnit
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.ApproveOrderListener
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.ui.card.validation.CardViewUiState
import com.paypal.android.utils.SharedPreferenceUtil
import com.paypal.checkout.createorder.OrderIntent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    private val args: CardFragmentArgs by navArgs()
    private val viewModel by viewModels<CardViewModel>()

    companion object {
        const val TAG = "CardFragment"
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
    }

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    @Inject
    lateinit var dataCollectorHandler: DataCollectorHandler

    private lateinit var cardClient: CardClient

    private var orderIntent: OrderIntent? = null

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        args.prefillCard?.card?.let {
            viewModel.updateCard(it)
        }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        CardView(
                            viewModel = viewModel,
                            onFormSubmit = { uiState -> approveOrder(uiState) })
                    }
                }
            }
        }
    }

    private fun approveOrder(uiState: CardViewUiState) {
        viewLifecycleOwner.lifecycleScope.launch {
            createOrder(uiState)
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun CardView(
        viewModel: CardViewModel,
        onFormSubmit: (CardViewUiState) -> Unit = {},
    ) {
        val scaOptionExpanded by viewModel.scaOptionExpanded.collectAsStateWithLifecycle(initialValue = false)
        val intentOptionExpanded by viewModel.intentOptionExpanded.collectAsStateWithLifecycle(initialValue = false)
        val shouldVaultOptionExpanded by viewModel.shouldVaultOptionExpanded.collectAsStateWithLifecycle(initialValue = false)

        val scaOption by viewModel.scaOption.collectAsStateWithLifecycle(initialValue = "")
        val intentOption by viewModel.intentOption.collectAsStateWithLifecycle(initialValue = "")
        val shouldVaultOption by viewModel.shouldVaultOption.collectAsStateWithLifecycle(initialValue = "")
        val customerId by viewModel.customerId.collectAsStateWithLifecycle(initialValue = "")

        val statusText by viewModel.statusText.collectAsStateWithLifecycle(initialValue = "")

        val cardNumber by viewModel.cardNumber.collectAsStateWithLifecycle(initialValue = "")
        val expirationDate by viewModel.expirationDate.collectAsStateWithLifecycle(initialValue = "")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = "Card Details",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(2.dp))
            CardInputView(cardNumber = cardNumber, expirationDate = expirationDate)
            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = "Approve Order Options",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(2.dp))
            OptionDropDown(
                hint = "SCA",
                value = scaOption,
                expanded = scaOptionExpanded,
                options = listOf("ALWAYS", "WHEN REQUIRED"),
                modifier = Modifier.fillMaxWidth(),
                onExpandedChange = { expanded ->
                    if (expanded) {
                        viewModel.onFocusChange(CardOption.SCA)
                    } else {
                        viewModel.clearFocus()
                    }
                },
                onValueChange = { value ->
                    viewModel.onOptionChange(CardOption.SCA, value)
                    viewModel.clearFocus()
                }
            )
            Spacer(modifier = Modifier.size(8.dp))
            OptionDropDown(
                hint = "INTENT",
                value = intentOption,
                expanded = intentOptionExpanded,
                options = listOf("AUTHORIZE", "CAPTURE"),
                modifier = Modifier.fillMaxWidth(),
                onExpandedChange = { expanded ->
                    if (expanded) {
                        viewModel.onFocusChange(CardOption.INTENT)
                    } else {
                        viewModel.clearFocus()
                    }
                },
                onValueChange = { value ->
                    viewModel.onOptionChange(CardOption.INTENT, value)
                    viewModel.clearFocus()
                }
            )
            Spacer(modifier = Modifier.size(8.dp))
            OptionDropDown(
                hint = "SHOULD VAULT",
                value = shouldVaultOption,
                options = listOf("YES", "NO"),
                expanded = shouldVaultOptionExpanded,
                modifier = Modifier.fillMaxWidth(),
                onExpandedChange = { expanded ->
                    if (expanded) {
                        viewModel.onFocusChange(CardOption.SHOULD_VAULT)
                    } else {
                        viewModel.clearFocus()
                    }
                },
                onValueChange = { value ->
                    viewModel.onOptionChange(CardOption.SHOULD_VAULT, value)
                    viewModel.clearFocus()
                }
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = customerId,
                label = { Text("CUSTOMER ID FOR VAULT") },
                onValueChange = { value ->
                    viewModel.onOptionChange(CardOption.CUSTOMER_VAULT_ID, value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused) {
                            viewModel.onFocusChange(CardOption.CUSTOMER_VAULT_ID)
                        }
                    }
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = statusText,
                modifier = Modifier.weight(1.0f)
            )
            OutlinedButton(
                onClick = {
                    onFormSubmit(viewModel.uiState.value)
                },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text("CREATE & APPROVE ORDER")
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }

    @ExperimentalMaterial3Api
    @Preview
    @Composable
    fun CardViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                CardView(viewModel = viewModel())
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun OptionDropDown(
        hint: String,
        value: String,
        options: List<String>,
        expanded: Boolean,
        modifier: Modifier,
        onExpandedChange: (Boolean) -> Unit,
        onValueChange: (String) -> Unit
    ) {
        // Ref: https://alexzh.com/jetpack-compose-dropdownmenu/
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            modifier = modifier
        ) {
            OutlinedTextField(
                value = value,
                label = { Text(hint) },
                readOnly = true,
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = {}) {
                options.forEach { item ->
                    DropdownMenuItem(text = { Text(text = item) }, onClick = {
                        onValueChange(item)
                    })
                }
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun CardInputView(cardNumber: String, expirationDate: String) {
        OutlinedTextField(
            value = cardNumber,
            label = { Text("CARD NUMBER") },
            onValueChange = {
                viewModel.onCardNumberChanged(it)
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            visualTransformation = CardNumberVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = expirationDate,
                label = { Text("EXP. DATE") },
                onValueChange = {
                    viewModel.onExpirationDateChanged(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2.0f)
            )
            OutlinedTextField(
                value = "",
                label = { Text("CVV") },
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        binding.run {
//            cardNumberInput.onValueChange = ::onCardNumberChange
//            cardExpirationInput.onValueChange = ::onCardExpirationDateChange
//
//            useTestCardButton.setOnClickListener {
//                findNavController().navigate(R.id.action_cardFragment_to_testCardFragment)
//            }
//            submitButton.setOnClickListener {
//                viewLifecycleOwner.lifecycleScope.launch {
//                    createOrder()
//                }
//            }
    }

    //
//        setFragmentResultListener(TestCardsFragment.REQUEST_KEY) { _, bundle ->
//            handleTestCardSelected(bundle)
//        }
//    }
//
//    private fun handleTestCardSelected(bundle: Bundle) {
//        val cardNumber = bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_NUMBER)
//        val securityCode = bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_SECURITY_CODE)
//
//        val expirationMonth =
//            bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_EXPIRATION_MONTH)
//        val expirationYear =
//            bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_EXPIRATION_YEAR)
//
//        binding.run {
//            cardNumberInput.setText("")
//            val formattedCardNumber =
//                CardFormatter.formatCardNumber(cardNumber ?: "")
//            cardNumberInput.setText(formattedCardNumber)
//
//            val expirationDate = "$expirationMonth/$expirationYear"
//            cardExpirationInput.setText(expirationDate)
//            cardSecurityCodeInput.setText(securityCode)
//        }
//    }
//
//    private fun onCardNumberChange(oldValue: String, newValue: String) {
//        val formattedCardNumber = CardFormatter.formatCardNumber(newValue, oldValue)
//        binding.cardNumberInput.setText(formattedCardNumber)
//        binding.cardNumberInput.setSelection(formattedCardNumber.length)
//    }
//
//    private fun onCardExpirationDateChange(oldValue: String, newValue: String) {
//        val formattedExpirationDate = DateFormatter.formatExpirationDate(newValue, oldValue)
//        binding.cardExpirationInput.setText(formattedExpirationDate)
//        binding.cardExpirationInput.setSelection(formattedExpirationDate.length)
//    }
//
    private suspend fun createOrder(uiState: CardViewUiState) {
        val orderIntent = when (uiState.intentOption) {
            "AUTHORIZE" -> OrderIntent.AUTHORIZE
            else -> OrderIntent.CAPTURE
        }

        val clientId = sdkSampleServerAPI.fetchClientId()
        val configuration = CoreConfig(clientId = clientId)
        cardClient = CardClient(requireActivity(), configuration)

        cardClient.approveOrderListener = object : ApproveOrderListener {
            override fun onApproveOrderSuccess(result: CardResult) {
                viewLifecycleOwner.lifecycleScope.launch {
                    when (orderIntent) {
                        OrderIntent.CAPTURE -> captureOrder(result)
                        OrderIntent.AUTHORIZE -> authorizeOrder(result)
                    }
                }
            }

            override fun onApproveOrderFailure(error: PayPalSDKError) {
                viewModel.updateStatusText("CAPTURE fail: ${error.errorDescription}")
            }

            override fun onApproveOrderCanceled() {
                viewModel.updateStatusText("USER CANCELED")
            }

            override fun onApproveOrderThreeDSecureWillLaunch() {
                viewModel.updateStatusText("3DS launched")
            }

            override fun onApproveOrderThreeDSecureDidFinish() {
                viewModel.updateStatusText("3DS finished")
            }
        }

        dataCollectorHandler.setLogging(true)
        viewModel.updateStatusText("Creating order...")

        val orderRequest = CreateOrderRequest(
            intent = orderIntent.name,
            purchaseUnit = listOf(
                PurchaseUnit(
                    amount = Amount(
                        currencyCode = "USD",
                        value = "10.99"
                    )
                )
            ),
            payee = Payee(emailAddress = "anpelaez@paypal.com")
        )

        val order = sdkSampleServerAPI.createOrder(orderRequest = orderRequest)

        val clientMetadataId = dataCollectorHandler.getClientMetadataId(order.id)
        Log.i(TAG, "MetadataId: $clientMetadataId")

        viewModel.updateStatusText("Authorizing order...")

        // build card request
        val card = args.prefillCard!!.card
        val sca = when (uiState.scaOption) {
            "ALWAYS" -> SCA.SCA_ALWAYS
            else -> SCA.SCA_WHEN_REQUIRED
        }

        val cardRequest = CardRequest(order.id!!, card, APP_RETURN_URL, sca)
        cardClient.approveOrder(requireActivity(), cardRequest)
    }

    private suspend fun captureOrder(cardResult: CardResult) {
        viewModel.updateStatusText("Capturing order with ID: ${cardResult.orderId}...")
        val result = sdkSampleServerAPI.captureOrder(cardResult.orderId)
        updateStatusTextWithCardResult(cardResult, result.status)
    }

    private suspend fun authorizeOrder(cardResult: CardResult) {
        viewModel.updateStatusText("Authorizing order with ID: ${cardResult.orderId}...")
        val result = sdkSampleServerAPI.authorizeOrder(cardResult.orderId)
        updateStatusTextWithCardResult(cardResult, result.status)
    }

    private fun updateStatusTextWithCardResult(result: CardResult, orderStatus: String?) {
        val statusText = "Confirmed Order: ${result.orderId} Status: $orderStatus"
        val deepLink = result.deepLinkUrl?.toString().orEmpty()
        val joinedText = listOf(statusText, deepLink).joinToString("\n")
        viewModel.updateStatusText(joinedText)
    }
}
