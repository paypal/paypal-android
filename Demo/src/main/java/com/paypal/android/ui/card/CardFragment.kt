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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.paypal.android.R
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
import com.paypal.android.ui.WireframeOptionDropDown
import com.paypal.android.ui.card.validation.CardViewUiState
import com.paypal.android.ui.stringResourceListOf
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

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        args.prefillCard?.card?.let { viewModel.prefillCard(it) }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        CardView(uiState = uiState, onFormSubmit = { approveOrder() })
                    }
                }
            }
        }
    }

    private fun approveOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            createOrder(viewModel.uiState.value)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @ExperimentalMaterial3Api
    @Composable
    fun CardView(
        uiState: CardViewUiState,
        onFormSubmit: () -> Unit = {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .semantics {
                    testTagsAsResourceId = true
                }
        ) {
            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = "Card Details",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(2.dp))
            CardForm(
                cardNumber = uiState.cardNumber,
                expirationDate = uiState.cardExpirationDate,
                securityCode = uiState.cardSecurityCode
            )
            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = "Approve Order Options",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(2.dp))
            ApproveOrderForm(uiState)
            Spacer(modifier = Modifier.size(8.dp))
            Column(
                modifier = Modifier.weight(1.0f)
            ) {
                Text(text = uiState.statusText, modifier = Modifier.testTag("statusText"))
                Text(uiState.orderDetails)
            }
            OutlinedButton(
                onClick = { onFormSubmit() },
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
    fun CardFormPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                CardView(uiState = CardViewUiState())
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun CardForm(cardNumber: String, expirationDate: String, securityCode: String) {
        OutlinedTextField(
            value = cardNumber,
            label = { Text(stringResource(id = R.string.card_field_card_number)) },
            onValueChange = { viewModel.onValueChange(CardOption.CARD_NUMBER, it) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            visualTransformation = CardNumberVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.isFocused) {
                        viewModel.onOptionFocus(CardOption.CARD_NUMBER)
                    }
                }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = expirationDate,
                label = { Text(stringResource(id = R.string.card_field_expiration)) },
                onValueChange = { viewModel.onValueChange(CardOption.CARD_EXPIRATION_DATE, it) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                visualTransformation = DateVisualTransformation(),
                modifier = Modifier
                    .weight(weight = 1.5f)
                    .onFocusChanged {
                        if (it.isFocused) {
                            viewModel.onOptionFocus(CardOption.CARD_EXPIRATION_DATE)
                        }
                    }
            )
            OutlinedTextField(
                value = securityCode,
                label = { Text(stringResource(id = R.string.card_field_security_code)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                onValueChange = { viewModel.onValueChange(CardOption.CARD_SECURITY_CODE, it) },
                modifier = Modifier
                    .weight(1.0f)
                    .onFocusChanged {
                        if (it.isFocused) {
                            viewModel.onOptionFocus(CardOption.CARD_SECURITY_CODE)
                        }
                    }
            )
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun ApproveOrderForm(uiState: CardViewUiState) {
        WireframeOptionDropDown(
            hint = stringResource(id = R.string.sca_title),
            value = uiState.scaOption,
            expanded = uiState.scaOptionExpanded,
            options = stringResourceListOf(R.string.sca_always, R.string.sca_when_required),
            modifier = Modifier.fillMaxWidth(),
            onExpandedChange = { expanded ->
                if (expanded) viewModel.onOptionFocus(CardOption.SCA) else viewModel.clearFocus()
            },
            onValueChange = {
                viewModel.onValueChange(CardOption.SCA, it)
                viewModel.clearFocus()
            }
        )
        Spacer(modifier = Modifier.size(8.dp))
        WireframeOptionDropDown(
            hint = stringResource(id = R.string.intent_title),
            value = uiState.intentOption,
            expanded = uiState.intentOptionExpanded,
            options = stringResourceListOf(R.string.intent_authorize, R.string.intent_capture),
            modifier = Modifier.fillMaxWidth(),
            onExpandedChange = { expanded ->
                if (expanded) viewModel.onOptionFocus(CardOption.INTENT) else viewModel.clearFocus()
            },
            onValueChange = {
                viewModel.onValueChange(CardOption.INTENT, it)
                viewModel.clearFocus()
            }
        )
        Spacer(modifier = Modifier.size(8.dp))
        WireframeOptionDropDown(
            hint = "SHOULD VAULT",
            value = uiState.shouldVaultOption,
            options = listOf("YES", "NO"),
            expanded = uiState.shouldVaultOptionExpanded,
            modifier = Modifier.fillMaxWidth(),
            onExpandedChange = { expanded ->
                if (expanded) viewModel.onOptionFocus(CardOption.SHOULD_VAULT) else viewModel.clearFocus()
            },
            onValueChange = {
                viewModel.onValueChange(CardOption.SHOULD_VAULT, it)
                viewModel.clearFocus()
            }
        )
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = uiState.customerId,
            label = { Text("CUSTOMER ID FOR VAULT") },
            onValueChange = { viewModel.onValueChange(CardOption.VAULT_CUSTOMER_ID, it) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.isFocused) {
                        viewModel.onOptionFocus(CardOption.VAULT_CUSTOMER_ID)
                    }
                }
        )
    }

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
                    finishOrder(result, orderIntent)
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
        val card = parseCard(uiState)
        val sca = when (uiState.scaOption) {
            "ALWAYS" -> SCA.SCA_ALWAYS
            else -> SCA.SCA_WHEN_REQUIRED
        }

        val cardRequest = CardRequest(order.id!!, card, APP_RETURN_URL, sca)
        cardClient.approveOrder(requireActivity(), cardRequest)
    }

    private fun parseCard(uiState: CardViewUiState): Card {
        // TODO: handle invalid date string
        var expirationMonth = ""
        var expirationYear = ""

        // expiration date in UI State needs to be formatted because it uses a visual transformation
        val dateString = DateString(uiState.cardExpirationDate)
        val dateStringComponents = dateString.formatted.split("/")
        if (dateStringComponents.isNotEmpty()) {
            expirationMonth = dateStringComponents[0]
            if (dateStringComponents.size > 1) {
                val rawYear = dateStringComponents[1]
                expirationYear = if (rawYear.length == 2) {
                    // pad with 20 to assume 2000's
                    "20$rawYear"
                } else {
                    rawYear
                }
            }
        }

        return Card(
            number = uiState.cardNumber,
            expirationMonth = expirationMonth,
            expirationYear = expirationYear,
            securityCode = uiState.cardSecurityCode
        )
    }

    private suspend fun finishOrder(cardResult: CardResult, orderIntent: OrderIntent) {
        val orderId = cardResult.orderId
        val finishResult = when (orderIntent) {
            OrderIntent.CAPTURE -> {
                viewModel.updateStatusText("Capturing order with ID: ${cardResult.orderId}...")
                sdkSampleServerAPI.captureOrder(orderId)
            }

            OrderIntent.AUTHORIZE -> {
                viewModel.updateStatusText("Authorizing order with ID: ${cardResult.orderId}...")
                sdkSampleServerAPI.authorizeOrder(orderId)
            }
        }

        viewModel.updateStatusText("Status: ${finishResult.status}")
        val orderDetailsText = "Confirmed Order: $orderId"
        val deepLink = cardResult.deepLinkUrl?.toString().orEmpty()
        val joinedText = listOf(orderDetailsText, deepLink).joinToString("\n")
        viewModel.updateOrderDetailsText(joinedText)
    }
}
