package com.paypal.android.ui.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
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
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.ui.OptionList
import com.paypal.android.ui.WireframeButton
import com.paypal.android.ui.card.validation.CardViewUiState
import com.paypal.android.ui.features.Feature
import com.paypal.android.ui.stringResourceListOf
import com.paypal.android.uishared.components.CardForm
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    companion object {
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
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

        val feature = args.feature
        if (feature == Feature.CARD_VAULT) {
            // the vault api only has the 'when required' option
            viewModel.scaOption = "WHEN REQUIRED"
        } else {
            viewModel.scaOption = "ALWAYS"
        }

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
                            onFormSubmit = { onFormSubmit() }
                        )
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
        val card = parseCard(viewModel.uiState.value)

        // TODO: implement vault without purchase
        AlertDialog.Builder(requireContext())
            .setTitle("TODO")
            .setMessage("Implement Vault Without Purchase")
            .setPositiveButton("OK") { _, _ -> }
            .show()
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
                securityCode = uiState.cardSecurityCode,
                onCardNumberChange = { viewModel.cardNumber = it },
                onExpirationDateChange = { viewModel.cardExpirationDate = it },
                onSecurityCodeChange = { viewModel.cardSecurityCode = it },
            )
            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = "${stringResource(feature.stringRes)} Options",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(8.dp))
            OptionsForm(uiState)
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
    fun CardViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                CardView(feature = Feature.CARD_APPROVE_ORDER, uiState = CardViewUiState())
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun OptionsForm(uiState: CardViewUiState) {
        val scaOptions = if (args.feature == Feature.CARD_VAULT) {
            stringResourceListOf(R.string.sca_when_required)
        } else {
            stringResourceListOf(R.string.sca_always, R.string.sca_when_required)
        }
        OptionList(
            title = stringResource(id = R.string.sca_title),
            options = scaOptions,
            selectedOption = uiState.scaOption,
            onOptionSelected = { scaOption -> viewModel.scaOption = scaOption }
        )
    }

    private fun createCardRequest(uiState: CardViewUiState, order: Order): CardRequest {
        val card = parseCard(uiState)
        val sca = when (uiState.scaOption) {
            "ALWAYS" -> SCA.SCA_ALWAYS
            else -> SCA.SCA_WHEN_REQUIRED
        }
        return CardRequest(order.id!!, card, APP_RETURN_URL, sca)
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
