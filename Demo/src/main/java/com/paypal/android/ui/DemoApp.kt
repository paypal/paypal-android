package com.paypal.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.paypal.android.ui.features.Feature
import com.paypal.android.ui.features.FeaturesView

@Composable
fun DemoApp() {
    MaterialTheme {
        val navController = rememberNavController()
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "features") {
                composable("features") {
                    FeaturesView(onFeatureSelected = { feature ->
                        navController.navigate(feature.routeName)
                    })
                }
                composable(Feature.CARD_APPROVE_ORDER.routeName) {

                }
            }
        }
    }
}