package com.paypal.android.ui.venmocheckout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.utils.UIConstants

@ExperimentalComposeUiApi
@Composable
fun VenmoCheckoutView(
    viewModel: VenmoCheckoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val contentPadding = UIConstants.paddingMedium
    Column(
        verticalArrangement = UIConstants.spacingLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding)
            .verticalScroll(scrollState)
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        Step1_LaunchVenmo(uiState)
    }
}

@Composable
private fun Step1_LaunchVenmo(uiState: VenmoCheckoutUiState) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Launch Venmo")
        ActionButtonColumn(
            defaultTitle = "LAUNCH VENMO",
            successTitle = "LAUNCH VENMO SUCCESS",
            state = uiState.venmoCheckoutState,
            onClick = {
                // TODO: launch venmo
            },
            modifier = Modifier
                .fillMaxWidth()
        ) { state ->
            // TODO: handle venmo result
        }
    }
}
