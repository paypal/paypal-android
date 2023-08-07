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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.ImeAction
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
import com.paypal.android.ui.WireframeButton
import com.paypal.android.ui.card.DateString
import com.paypal.android.uishared.components.CardForm
import com.paypal.android.uishared.components.PaymentTokenView
import com.paypal.android.uishared.components.SetupTokenView
import com.paypal.android.usecase.CreatePaymentTokenUseCase
import com.paypal.android.usecase.CreateSetupTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
                            onUpdateSetupTokenSubmit = { updateSetupToken() },
                            onCreatePaymentTokenSubmit = { createPaymentToken() },
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

    private fun updateSetupToken() {
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
        onUpdateSetupTokenSubmit: () -> Unit,
        onCreatePaymentTokenSubmit: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            CreateSetupTokenView(
                uiState = uiState,
                onSubmit = { onCreateSetupTokenSubmit() }
            )
            uiState.setupToken?.let { setupToken ->
                Spacer(modifier = Modifier.size(8.dp))
                SetupTokenView(setupToken = setupToken)
                Spacer(modifier = Modifier.size(8.dp))
                UpdateSetupTokenView(
                    uiState = uiState,
                    onCardNumberChange = { viewModel.cardNumber = it },
                    onExpirationDateChange = { viewModel.cardExpirationDate = it },
                    onSecurityCodeChange = { viewModel.cardSecurityCode = it },
                    onSubmit = { onUpdateSetupTokenSubmit() }
                )
            }
            uiState.vaultResult?.let { vaultResult ->
                Spacer(modifier = Modifier.size(8.dp))
                CreatePaymentTokenView(
                    uiState = uiState,
                    onSubmit = { onCreatePaymentTokenSubmit() }
                )
            }
            uiState.paymentToken?.let { paymentToken ->
                Spacer(modifier = Modifier.size(8.dp))
                PaymentTokenView(paymentToken = paymentToken)
            }
        }
    }

    @Composable
    fun CreateSetupTokenView(
        uiState: VaultUiState,
        onSubmit: () -> Unit
    ) {
        val localFocusManager = LocalFocusManager.current
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Vault without purchase requires a setup token:",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.size(16.dp))
                OutlinedTextField(
                    value = uiState.customerId,
                    label = { Text("VAULT CUSTOMER ID (OPTIONAL)") },
                    onValueChange = { value -> viewModel.customerId = value },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { localFocusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.size(8.dp))
                WireframeButton(
                    text = "Create Setup Token",
                    isLoading = uiState.isCreateSetupTokenLoading,
                    onClick = { onSubmit() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UpdateSetupTokenView(
        uiState: VaultUiState,
        onCardNumberChange: (String) -> Unit,
        onExpirationDateChange: (String) -> Unit,
        onSecurityCodeChange: (String) -> Unit,
        onSubmit: () -> Unit
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Vault Card",
                    style = MaterialTheme.typography.headlineSmall
                )
                CardForm(
                    cardNumber = uiState.cardNumber,
                    expirationDate = uiState.cardExpirationDate,
                    securityCode = uiState.cardSecurityCode,
                    onCardNumberChange = { onCardNumberChange(it) },
                    onExpirationDateChange = { onExpirationDateChange(it) },
                    onSecurityCodeChange = { onSecurityCodeChange(it) },
                )
                Spacer(modifier = Modifier.size(8.dp))
                WireframeButton(
                    text = "Vault Card",
                    isLoading = uiState.isUpdateSetupTokenLoading,
                    onClick = { onSubmit() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    fun CreatePaymentTokenView(
        uiState: VaultUiState,
        onSubmit: () -> Unit
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Create a Permanent Payment Method Token",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.size(8.dp))
                WireframeButton(
                    text = "Create Payment Token",
                    isLoading = uiState.isCreatePaymentTokenLoading,
                    onClick = { onSubmit() },
                    modifier = Modifier.fillMaxWidth()
                )
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
                    onUpdateSetupTokenSubmit = {},
                    onCreatePaymentTokenSubmit = {},
                )
            }
        }
    }
}