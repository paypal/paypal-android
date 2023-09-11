package com.paypal.android.ui.createorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.ui.features.Feature
import com.paypal.android.uishared.components.CreateOrderWithVaultOptionForm
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
            viewModel.isLoading = true

            val uiState = viewModel.uiState.value
            val order = uiState.run {
                sdkSampleServerAPI.createOrder(
                    orderIntent = intentOption,
                    shouldVault = shouldVault,
                    vaultCustomerId = customerId
                )
            }
            viewModel.isLoading = false

            // TODO: remove once Feature enum is converted to an inner class of FeaturesFragment
            // continue on to feature
            when (val feature = args.feature) {
                Feature.PAYPAL_NATIVE -> {
                    navigate(
                        CreateOrderFragmentDirections.actionCreateOrderFragmentToPayPalNativeFragment(
                            order
                        )
                    )
                }
                else -> {}
            }
        }
    }

    private fun navigate(action: NavDirections) {
        findNavController().navigate(action)
    }

    @Composable
    fun CreateOrderView(
        feature: Feature,
        uiState: CreateOrderUiState,
        onCreateOrderClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            CreateOrderWithVaultOptionForm(
                title = "Create an order to proceed with ${stringResource(feature.stringRes)}:",
                orderIntent = uiState.intentOption,
                shouldVault = uiState.shouldVault,
                vaultCustomerId = uiState.customerId,
                isLoading = uiState.isLoading,
                onIntentOptionSelected = { value -> viewModel.intentOption = value },
                onShouldVaultChanged = { value -> viewModel.shouldVault = value },
                onVaultCustomerIdChanged = { value -> viewModel.customerId = value },
                onSubmit = { onCreateOrderClick() }
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
