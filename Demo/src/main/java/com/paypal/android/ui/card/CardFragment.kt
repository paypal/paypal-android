package com.paypal.android.ui.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.paypal.android.R
import com.paypal.android.api.model.Order
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.Vault
import com.paypal.android.cardpayments.VaultRequest
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.ui.OptionList
import com.paypal.android.ui.WireframeButton
import com.paypal.android.ui.card.validation.CardViewUiState
import com.paypal.android.ui.features.Feature
import com.paypal.android.ui.stringResourceListOf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    companion object {
        // TODO: investigate why custom url-schemes don't work with the setup token endpoint
         const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
//        const val APP_RETURN_URL = "https://example.com/returnUrl"
    }

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    private val args: CardFragmentArgs by navArgs()
    private val viewModel by viewModels<CardViewModel>()

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
                        val feature = args.feature
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        CardView(
                            feature = feature,
                            uiState = uiState,
                            onFormSubmit = { onFormSubmit() })
                    }
                }
            }
        }
    }

    private fun onFormSubmit() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (args.feature) {
                Feature.CARD_APPROVE_ORDER -> {
                    sendApproveOrderRequest()
                }

                Feature.CARD_VAULT -> {
                    sendVaultRequest()
                }

                else -> {
                    TODO("invalid state")
                }
            }
        }
    }

    private fun sendApproveOrderRequest() {
        val order = args.order
        if (order == null) {
            // TODO: handle invalid state
        }
        val uiState = viewModel.uiState.value
        val cardRequest = createCardRequest(uiState, order!!)
        findNavController().navigate(
            CardFragmentDirections.actionCardFragmentToApproveOrderProgressFragment(cardRequest = cardRequest)
        )
    }

    private fun sendVaultRequest() {
        val uiState = viewModel.uiState.value
        val card = parseCard(uiState)
        val customerId = uiState.customerId
        val vaultRequest = VaultRequest(card, APP_RETURN_URL, customerId)
        findNavController().navigate(
            CardFragmentDirections.actionCardFragmentToApproveOrderProgressFragment(vaultRequest = vaultRequest)
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @ExperimentalMaterial3Api
    @Composable
    fun CardView(
        feature: Feature,
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
                text = "${stringResource(feature.stringRes)} Options",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(8.dp))
            ApproveOrderForm(uiState)
            Spacer(modifier = Modifier.weight(1.0f))
            WireframeButton(
                text = "CREATE & APPROVE ORDER",
                onClick = { onFormSubmit() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.size(24.dp))
        }
    }

    @ExperimentalMaterial3Api
    @Preview
    @Composable
    fun CardFormPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                CardView(feature = Feature.CARD_APPROVE_ORDER, uiState = CardViewUiState())
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun CardForm(cardNumber: String, expirationDate: String, securityCode: String) {
        OutlinedTextField(
            value = cardNumber,
            label = { Text(stringResource(id = R.string.card_field_card_number)) },
            onValueChange = { value -> viewModel.cardNumber = value },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            visualTransformation = CardNumberVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = expirationDate,
                label = { Text(stringResource(id = R.string.card_field_expiration)) },
                onValueChange = { value -> viewModel.cardExpirationDate = value },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                visualTransformation = DateVisualTransformation(),
                modifier = Modifier.weight(weight = 1.5f)
            )
            OutlinedTextField(
                value = securityCode,
                label = { Text(stringResource(id = R.string.card_field_security_code)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                onValueChange = { value -> viewModel.cardSecurityCode = value },
                modifier = Modifier.weight(1.0f)
            )
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun ApproveOrderForm(uiState: CardViewUiState) {
        val localFocusManager = LocalFocusManager.current
        OptionList(
            title = stringResource(id = R.string.sca_title),
            options = stringResourceListOf(R.string.sca_always, R.string.sca_when_required),
            selectedOption = uiState.scaOption,
            onOptionSelected = { scaOption -> viewModel.scaOption = scaOption }
        )
        Spacer(modifier = Modifier.size(16.dp))
        Row {
            Text(
                text = "Should Vault",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1.0f)
            )
            Switch(
                checked = uiState.shouldVault,
                onCheckedChange = { shouldVault -> viewModel.shouldVault = shouldVault }
            )
        }
        OutlinedTextField(
            value = uiState.customerId,
            label = { Text("VAULT CUSTOMER ID (OPTIONAL)") },
            onValueChange = { value -> viewModel.customerId = value },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { localFocusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )
    }

    private fun createCardRequest(uiState: CardViewUiState, order: Order): CardRequest {
        val card = parseCard(uiState)
        val sca = when (uiState.scaOption) {
            "ALWAYS" -> SCA.SCA_ALWAYS
            else -> SCA.SCA_WHEN_REQUIRED
        }

        val vault = if (uiState.shouldVault) Vault(customerId = uiState.customerId) else null
        return CardRequest(order.id!!, card, APP_RETURN_URL, sca, vault)
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
}
