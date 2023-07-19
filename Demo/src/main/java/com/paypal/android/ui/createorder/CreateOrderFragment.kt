package com.paypal.android.ui.createorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
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
import com.paypal.android.cardpayments.OrderIntent
import com.paypal.android.ui.WireframeButton
import com.paypal.android.ui.WireframeOptionDropDown
import com.paypal.android.ui.features.Feature
import com.paypal.android.ui.stringResourceListOf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun CreateOrderView(
        feature: Feature,
        uiState: CreateOrderUiState,
        onCreateOrderClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
        ) {
            Text(text = "Create Order for ${stringResource(feature.stringRes)}")
            WireframeOptionDropDown(
                hint = stringResource(id = R.string.intent_title),
                value = uiState.intentOption,
                expanded = uiState.intentOptionExpanded,
                options = stringResourceListOf(R.string.intent_authorize, R.string.intent_capture),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                onExpandedChange = { expanded ->
                    viewModel.intentOptionExpanded = expanded
                },
                onValueChange = { value ->
                    viewModel.intentOption = value
                    viewModel.intentOptionExpanded = false
                }
            )
            WireframeButton(
                text = "Create Order & Continue",
                onClick = onCreateOrderClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
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