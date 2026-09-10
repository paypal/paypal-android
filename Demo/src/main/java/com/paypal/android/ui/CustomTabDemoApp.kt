package com.paypal.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import com.paypal.android.BuildConfig
import com.paypal.android.DemoActivityType
import com.paypal.android.customenvironment.SettingsView
import com.paypal.android.customenvironment.SettingsViewModel
import com.paypal.android.ui.paypal.PayPalCheckoutView
import com.paypal.android.ui.paypal.PayPalCheckoutViewModel
import com.paypal.android.uishared.components.DemoAppTopBar

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun CustomTabDemoApp(
    checkoutViewModel: PayPalCheckoutViewModel,
    settingsViewModel: SettingsViewModel,
    onSwitchActivityType: () -> Unit,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                DemoAppTopBar(
                    title = if (showSettings) "Settings" else "PayPal Checkout",
                    shouldDisplayBackButton = showSettings,
                    onBackButtonClick = { showSettings = false },
                    onSettingsClick = if (BuildConfig.DEBUG && !showSettings) {
                        { showSettings = true }
                    } else {
                        null
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showSettings) {
                    SettingsView(
                        activityType = DemoActivityType.PLAIN_ACTIVITY,
                        onSwitchActivityType = onSwitchActivityType,
                        viewModel = settingsViewModel,
                    )
                } else {
                    PayPalCheckoutView(
                        viewModel = checkoutViewModel,
                    )
                }
            }
        }
    }
}
