package com.paypal.android.ui.googlepay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivityOrNull

@Composable
fun GooglePayView(
    viewModel: GooglePayViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val contentPadding = UIConstants.paddingMedium
    Column(
        verticalArrangement = UIConstants.spacingLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding)
            .verticalScroll(scrollState)
    ) {
        Step1_LaunchGooglePay(viewModel = viewModel)
    }
}

@Composable
private fun Step1_LaunchGooglePay(viewModel: GooglePayViewModel) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create an Order")
        Button(
            onClick = {
                context.getActivityOrNull()?.let { viewModel.launchGooglePay(it) }
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Launch GooglePay", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Preview
@Composable
fun GooglePayViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            GooglePayView()
        }
    }
}