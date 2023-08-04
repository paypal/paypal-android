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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.paypal.android.ui.WireframeButton
import com.paypal.android.uishared.components.CardForm
import com.paypal.android.usecase.CreateSetupTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
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
                            onCreateSetupTokenSubmit = { createSetupToken() },
                            onUpdateSetupTokenSubmit = { updateSetupToken() }
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
            // TODO: call SDK vault method
            viewModel.isUpdateSetupTokenLoading = false
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun VaultView(
        uiState: VaultUiState,
        onCreateSetupTokenSubmit: () -> Unit,
        onUpdateSetupTokenSubmit: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            CreateSetupTokenView(
                uiState = uiState,
                onSubmit = { onCreateSetupTokenSubmit() }
            )
            if (uiState.setupToken.isNotEmpty()) {
                Spacer(modifier = Modifier.size(8.dp))
                UpdateSetupTokenView(
                    uiState = uiState,
                    onCardNumberChange = { viewModel.cardNumber = it },
                    onExpirationDateChange = { viewModel.cardExpirationDate = it },
                    onSecurityCodeChange = { viewModel.cardSecurityCode = it },
                    onSubmit = { onUpdateSetupTokenSubmit() }
                )
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

    @ExperimentalMaterial3Api
    @Preview
    @Composable
    fun VaultViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                VaultView(
                    uiState = VaultUiState(setupToken = "123"),
                    onCreateSetupTokenSubmit = {},
                    onUpdateSetupTokenSubmit = {},
                )
            }
        }
    }
}