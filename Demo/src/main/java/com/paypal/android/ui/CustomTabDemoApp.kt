package com.paypal.android.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paypal.android.BuildConfig
import com.paypal.android.DemoActivityType
import com.paypal.android.customenvironment.SettingsView
import com.paypal.android.customenvironment.SettingsViewModel
import com.paypal.android.ui.features.Feature
import com.paypal.android.ui.features.FeaturesView
import com.paypal.android.ui.paypal.PayPalCheckoutView
import com.paypal.android.ui.paypal.PayPalCheckoutViewModel
import com.paypal.android.uishared.components.DemoAppTopBar
import com.paypal.android.uishared.effects.NavDestinationChangeDisposableEffect
import com.paypal.android.utils.UIConstants

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun CustomTabDemoApp(
    checkoutViewModel: PayPalCheckoutViewModel,
    settingsViewModel: SettingsViewModel,
    onSwitchActivityType: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var shouldDisplayBackButton by remember { mutableStateOf(false) }

    NavDestinationChangeDisposableEffect(navController) { controller ->
        shouldDisplayBackButton = controller.previousBackStackEntry != null
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                val route = navBackStackEntry?.destination?.route
                DemoAppTopBar(
                    title = DemoAppDestinations.titleForDestination(route),
                    shouldDisplayBackButton = shouldDisplayBackButton,
                    onBackButtonClick = {
                        val destinationId = navController.graph.startDestinationId
                        navController.popBackStack(destinationId, false)
                    },
                    onSettingsClick = if (BuildConfig.DEBUG && route != DemoAppDestinations.SETTINGS) {
                        { navController.navigate(DemoAppDestinations.SETTINGS) }
                    } else {
                        null
                    },
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = DemoAppDestinations.FEATURES_ROUTE,
                enterTransition = {
                    fadeIn() + slideInVertically { UIConstants.getSlideInStartOffsetY(it) }
                },
                exitTransition = { fadeOut() },
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(DemoAppDestinations.FEATURES_ROUTE) {
                    FeaturesView(onSelectedFeatureChange = { feature ->
                        if (feature == Feature.PAYPAL_CHECKOUT) {
                            navController.navigate(feature.routeName)
                        }
                    })
                }
                composable(DemoAppDestinations.PAYPAL_CHECKOUT) {
                    PayPalCheckoutView(viewModel = checkoutViewModel)
                }
                if (BuildConfig.DEBUG) {
                    composable(DemoAppDestinations.SETTINGS) {
                        SettingsView(
                            activityType = DemoActivityType.PLAIN_ACTIVITY,
                            onSwitchActivityType = onSwitchActivityType,
                            viewModel = settingsViewModel,
                        )
                    }
                }
            }
        }
    }
}
