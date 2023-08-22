package com.paypal.android.ui.vault

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.paypal.android.api.model.PaymentToken
import com.paypal.android.api.model.SetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.VaultListener
import com.paypal.android.cardpayments.VaultRequest
import com.paypal.android.cardpayments.VaultResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.ui.card.CreateOrderView
import com.paypal.android.ui.card.DateString
import com.paypal.android.uishared.components.CreateOrderWithoutVaultForm
import com.paypal.android.uishared.components.PaymentTokenView
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.uishared.components.SetupTokenView
import com.paypal.android.usecase.CreatePaymentTokenUseCase
import com.paypal.android.usecase.CreateSetupTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class VaultFragment : Fragment() {

    @Inject
    lateinit var createPaymentTokenUseCase: CreatePaymentTokenUseCase

    @Inject
    lateinit var createSetupTokenUseCase: CreateSetupTokenUseCase

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    private lateinit var cardClient: CardClient

    private val args: VaultFragmentArgs by navArgs()
    private val viewModel by viewModels<VaultViewModel>()

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
                        VaultView(
                            uiState = uiState,
                            onCreateSetupTokenSubmit = { createSetupToken() },
                            onAttachCardToSetupTokenSubmit = { attachCardToSetupToken() },
                            onCreatePaymentTokenSubmit = { createPaymentToken() },
                            onCreateOrderSubmit = { createOrder() }
                        )
                    }
                }
            }
        }
    }

    private fun createSetupToken() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCreateSetupTokenLoading = true
            viewModel.setupToken = createSetupTokenUseCase(viewModel.customerId)
            viewModel.isCreateSetupTokenLoading = false
        }
    }

    private fun attachCardToSetupToken() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isUpdateSetupTokenLoading = true
            val clientId = sdkSampleServerAPI.fetchClientId()

            val configuration = CoreConfig(clientId = clientId)
            cardClient = CardClient(requireActivity(), configuration)
            cardClient.vaultListener = object : VaultListener {
                override fun onVaultSuccess(result: VaultResult) {
                    viewModel.isUpdateSetupTokenLoading = false
                    viewModel.vaultResult = result
                }

                override fun onVaultFailure(error: PayPalSDKError) {
                    viewModel.isUpdateSetupTokenLoading = false
                    // TODO: handle error
                }
            }

            val card = parseCard(viewModel.uiState.value)
            val vaultRequest = VaultRequest(viewModel.setupToken!!.id, card)
            cardClient.vault(requireContext(), vaultRequest)
        }
    }

    private fun createPaymentToken() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCreatePaymentTokenLoading = true
            viewModel.paymentToken = createPaymentTokenUseCase(viewModel.setupToken!!)
            viewModel.isCreatePaymentTokenLoading = false
        }
    }

    private fun createOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCreateOrderLoading = true
            val uiState = viewModel.uiState.value
            val jsonBody = buildCreateOrderWithPaymentTokenBody(uiState)
            viewModel.createdOrder = sdkSampleServerAPI.createOrder(jsonBody)
            viewModel.isCreateOrderLoading = false
        }
    }

    private fun buildCreateOrderWithPaymentTokenBody(uiState: VaultUiState): JSONObject {
        val amountJSON = JSONObject()
            .put("currency_code", "USD")
            .put("value", "10.99")

        val purchaseUnitJSON = JSONObject()
            .put("amount", amountJSON)

        val orderIntent = uiState.orderIntent
        val orderRequest = JSONObject()
            .put("intent", orderIntent)
            .put("purchase_units", JSONArray().put(purchaseUnitJSON))

        val tokenJSON = JSONObject()
            .put("id", uiState.paymentToken!!.id)
            .put("type", "PAYMENT_METHOD_TOKEN")

        val paymentSourceJSON = JSONObject()
            .put("token", tokenJSON)

        orderRequest.put("payment_source", paymentSourceJSON)
        return orderRequest
    }

    private fun parseCard(uiState: VaultUiState): Card {
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

    @ExperimentalMaterial3Api
    @Composable
    fun VaultView(
        uiState: VaultUiState,
        onCreateSetupTokenSubmit: () -> Unit,
        onAttachCardToSetupTokenSubmit: () -> Unit,
        onCreatePaymentTokenSubmit: () -> Unit,
        onCreateOrderSubmit: () -> Unit
    ) {
        val scrollState = rememberScrollState()
        LaunchedEffect(uiState) {
            // continuously scroll to bottom of the list when event state is updated
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .verticalScroll(scrollState)
        ) {
            CreateSetupTokenForm(
                uiState = uiState,
                onCustomerIdValueChange = { value -> viewModel.customerId = value },
                onSubmit = { onCreateSetupTokenSubmit() }
            )
            uiState.setupToken?.let { setupToken ->
                Spacer(modifier = Modifier.size(8.dp))
                SetupTokenView(setupToken = setupToken)
                Spacer(modifier = Modifier.size(8.dp))
                AttachCardToSetupTokenForm(
                    uiState = uiState,
                    onCardNumberChange = { viewModel.cardNumber = it },
                    onExpirationDateChange = { viewModel.cardExpirationDate = it },
                    onSecurityCodeChange = { viewModel.cardSecurityCode = it },
                    onSubmit = { onAttachCardToSetupTokenSubmit() }
                )
            }
            uiState.vaultResult?.let { vaultResult ->
                Spacer(modifier = Modifier.size(8.dp))
                VaultSuccessView(vaultResult = vaultResult)
                Spacer(modifier = Modifier.size(8.dp))
                CreatePaymentTokenForm(
                    uiState = uiState,
                    onSubmit = { onCreatePaymentTokenSubmit() }
                )
            }
            uiState.paymentToken?.let { paymentToken ->
                Spacer(modifier = Modifier.size(8.dp))
                PaymentTokenView(paymentToken = paymentToken)
                Spacer(modifier = Modifier.size(8.dp))
                CreateOrderWithoutVaultForm(
                    title = "Create Order",
                    orderIntent = uiState.orderIntent,
                    isLoading = uiState.isCreateOrderLoading,
                    onIntentOptionSelected = { value -> viewModel.orderIntent = value },
                    onSubmit = { onCreateOrderSubmit() }
                )
            }
            uiState.createdOrder?.let { createdOrder ->
                Spacer(modifier = Modifier.size(8.dp))
                CreateOrderView(order = createdOrder)
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }

    @Composable
    fun VaultSuccessView(vaultResult: VaultResult) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Vault Success",
                    style = MaterialTheme.typography.titleLarge,
                )
                PropertyView(name = "Setup Token Id", value = vaultResult.setupTokenId)
                PropertyView(name = "Status", value = vaultResult.status)
            }
        }
    }

    @ExperimentalMaterial3Api
    @Preview
    @Composable
    fun VaultViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                VaultView(
                    uiState = VaultUiState(
                        setupToken = SetupToken(
                            id = "fake-setup-token-id",
                            customerId = "fake-customer-id",
                            status = "fake-setup-token-status"
                        ),
                        paymentToken = PaymentToken(
                            "fake-payment-token-id",
                            "fake-customer-id",
                            "1234",
                            "fake-card-brand"
                        ),
                        vaultResult = VaultResult("456", "fake-status")
                    ),
                    onCreateSetupTokenSubmit = {},
                    onAttachCardToSetupTokenSubmit = {},
                    onCreatePaymentTokenSubmit = {},
                    onCreateOrderSubmit = {}
                )
            }
        }
    }
}
