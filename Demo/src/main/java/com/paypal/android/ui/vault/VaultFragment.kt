package com.paypal.android.ui.vault

import android.os.Bundle
import android.view.LayoutInflater
import androidx.compose.runtime.getValue
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.paypal.android.ui.WireframeButton
import com.paypal.android.uishared.components.CardForm
import com.paypal.android.usecase.CreateSetupTokenUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

class VaultFragment : Fragment() {

    @Inject
    lateinit var createSetupTokenUseCase: CreateSetupTokenUseCase

    private val viewModel by viewModels<VaultViewModel>()

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        VaultView(
                            uiState = uiState,
                            onCreateSetupTokenSubmit = { createSetupToken() }
                        )
                    }
                }
            }
        }
    }

    private fun createSetupToken() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSetupTokenLoading = true
            viewModel.setupToken = createSetupTokenUseCase(viewModel.customerId)
            viewModel.isSetupTokenLoading = false
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @ExperimentalMaterial3Api
    @Composable
    fun VaultView(
        uiState: VaultUiState,
        onCreateSetupTokenSubmit: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            SetupTokenRequestView(
                uiState = uiState,
                onSubmit = { onCreateSetupTokenSubmit() }
            )
            if (uiState.setupToken.isNotEmpty()) {
                CardForm(
                    cardNumber = "",
                    expirationDate = "",
                    securityCode = "",
                    onCardNumberChange = {},
                    onExpirationDateChange = {},
                    onSecurityCodeChange = {}
                )
            }
        }
    }

    @Composable
    fun SetupTokenRequestView(
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
                    isLoading = uiState.isSetupTokenLoading,
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
                    uiState = VaultUiState(),
                    onCreateSetupTokenSubmit = {}
                )
            }
        }
    }
}