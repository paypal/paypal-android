package com.paypal.android.ui.createorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.paypal.android.R
import com.paypal.android.api.model.Amount
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Payee
import com.paypal.android.api.model.PurchaseUnit
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.OrderIntent
import com.paypal.android.ui.OptionList
import com.paypal.android.ui.WireframeButton
import com.paypal.android.ui.features.Feature
import com.paypal.android.ui.stringResourceListOf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class CreateOrderFragment : Fragment() {

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    private val args: CreateOrderFragmentArgs by navArgs()
    private val viewModel by viewModels<CreateOrderViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CreateOrderView(
                        feature = args.feature,
                        uiState = uiState,
                        onCreateOrderClick = {
                            createOrder()
                        }
                    )
                }
            }
        }
    }

    private fun createOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            val uiState = viewModel.uiState.value
            val orderIntent = when (uiState.intentOption) {
                "AUTHORIZE" -> OrderIntent.AUTHORIZE
                else -> OrderIntent.CAPTURE
            }

            val amountJSON = JSONObject()
                .put("currency_code", "USD")
                .put("value", "10.99")

            val purchaseUnitJSON = JSONObject()
                .put("amount", amountJSON)

            val orderRequest = JSONObject()
                .put("intent", orderIntent)
                .put("purchase_units", JSONArray(purchaseUnitJSON))

            if (uiState.shouldVault) {
                val vaultJSON = JSONObject()
                    .put("store_in_vault", "ON_SUCCESS")

                val cardAttributesJSON = JSONObject()
                    .put("vault", vaultJSON)

                val customerId = uiState.customerId
                if (customerId.isNotEmpty()) {
                    val customerJSON = JSONObject()
                        .put("id", customerId)
                    cardAttributesJSON.put("customer", customerJSON)
                }

                val cardJSON = JSONObject()
                    .put("attributes", cardAttributesJSON)

                val paymentSourceJSON = JSONObject()
                    .put("card", cardJSON)

                orderRequest.put("payment_source", paymentSourceJSON)
            }

            viewModel.isLoading = true
            val order = sdkSampleServerAPI.createOrder(orderRequest = orderRequest)
            viewModel.isLoading = false

            // continue on to feature
            when (val feature = args.feature) {
                Feature.CARD_APPROVE_ORDER,
                Feature.CARD_VAULT -> {
                    navigate(
                        CreateOrderFragmentDirections.actionCreateOrderFragmentToSelectCardFragment(
                            feature,
                            order
                        )
                    )
                }

                Feature.PAYPAL_WEB -> {
                    navigate(
                        CreateOrderFragmentDirections.actionCreateOrderFragmentToPayPalFragment(
                            order
                        )
                    )
                }

                Feature.PAYPAL_NATIVE -> {
                    navigate(
                        CreateOrderFragmentDirections.actionCreateOrderFragmentToPayPalNativeFragment(
                            order
                        )
                    )
                }
            }
        }
    }

    private fun navigate(action: NavDirections) {
        findNavController().navigate(action)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CreateOrderView(
        feature: Feature,
        uiState: CreateOrderUiState,
        onCreateOrderClick: () -> Unit
    ) {
        val localFocusManager = LocalFocusManager.current
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = "The ${stringResource(feature.stringRes)} payment method requires an order to proceed.",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.size(16.dp))
            OptionList(
                title = stringResource(id = R.string.intent_title),
                options = stringResourceListOf(R.string.intent_authorize, R.string.intent_capture),
                selectedOption = uiState.intentOption,
                onOptionSelected = { option ->
                    viewModel.intentOption = option
                }
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
            WireframeButton(
                text = "Create Order & Continue",
                isLoading = uiState.isLoading,
                onClick = onCreateOrderClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }

    @Preview
    @Composable
    fun CreateOrderViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                CreateOrderView(
                    feature = Feature.CARD_APPROVE_ORDER,
                    uiState = CreateOrderUiState(),
                    onCreateOrderClick = {}
                )
            }
        }
    }
}
