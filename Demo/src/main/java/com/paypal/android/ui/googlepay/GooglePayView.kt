package com.paypal.android.ui.googlepay

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.wallet.contract.TaskResultContracts
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivityOrNull
import kotlinx.coroutines.launch

@Composable
fun GooglePayView(
    viewModel: GooglePayViewModel = hiltViewModel()
) {
    // Ref: https://stackoverflow.com/a/67156998
    val googlePayLauncher =
        rememberLauncherForActivityResult(TaskResultContracts.GetPaymentDataResult()) { taskResult ->
            taskResult?.let { viewModel.completeGooglePayLaunch(it) }
        }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val scrollState = rememberScrollState()
    val contentPadding = UIConstants.paddingMedium
    Column(
        verticalArrangement = UIConstants.spacingLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding)
            .verticalScroll(scrollState)
    ) {
        Step1_LaunchGooglePay(
            onLaunchGooglePay = {
                context.getActivityOrNull()?.let { activity ->
                    coroutineScope.launch {
                        // Ref: https://developers.google.com/pay/api/android/guides/tutorial#initiate-payment
                        val task = viewModel.launchGooglePay(activity)
                        task.addOnCompleteListener(googlePayLauncher::launch)
                    }
                }
            }
        )
    }
}

@Composable
private fun Step1_LaunchGooglePay(onLaunchGooglePay: () -> Unit) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create an Order")
        Button(
            onClick = onLaunchGooglePay,
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