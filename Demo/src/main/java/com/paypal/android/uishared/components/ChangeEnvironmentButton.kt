package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.datastore.environmentSettingsDataStore
import kotlinx.coroutines.flow.map

@Composable
fun ChangeEnvironmentButton(onClick: () -> Unit) {
    val context = LocalContext.current
    val activeEnvironment by context.environmentSettingsDataStore.data.map { environmentSettings ->
        environmentSettings.run { getEnvironments(activeEnvironmentIndex) }
    }.collectAsStateWithLifecycle(null)
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column {
            Text(text = "Environment")
            Text(text = activeEnvironment?.name ?: "Loading")
        }
    }

}