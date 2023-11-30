package com.paypal.android.uishared.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController

@Composable
fun NavDestinationChangeDisposableEffect(
    navController: NavController,
    onDestinationChange: (controller: NavController) -> Unit
) {
    // Ref: https://stackoverflow.com/a/68700967
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { controller, _, _ ->
            onDestinationChange(controller)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}
