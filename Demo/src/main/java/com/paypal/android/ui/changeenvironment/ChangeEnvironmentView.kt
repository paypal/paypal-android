package com.paypal.android.ui.changeenvironment

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.datastore.environmentSettingsDataStore
import com.paypal.android.uishared.components.OptionList
import com.paypal.android.utils.UIConstants
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

const val OPTION_CREATE_NEW = "Create New"

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ChangeEnvironmentView(onEnvironmentSelected: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val availableEnvironments by context.environmentSettingsDataStore.data.map { environmentSettings ->
        environmentSettings.environmentsList
    }.collectAsStateWithLifecycle(null)

    val activeEnvironment by context.environmentSettingsDataStore.data.map { environmentSettings ->
        environmentSettings.run { getEnvironments(activeEnvironmentIndex) }
    }.collectAsStateWithLifecycle(null)

    var selectedOption by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeEnvironment) {
        // make active environment the default option once it's loaded from shared prefs
        val activeEnvironmentName = activeEnvironment?.name
        if (selectedOption == null && activeEnvironmentName != null) {
            selectedOption = activeEnvironmentName
        }
    }

    val staticOptions = listOf(OPTION_CREATE_NEW)
    Column(
        verticalArrangement = UIConstants.spacingLarge,
        modifier = Modifier
            .padding(horizontal = UIConstants.paddingMedium)
    ) {
        OptionList(
            title = "Environment Options",
            options = (availableEnvironments?.map { it.name } ?: emptyList()) + staticOptions,
            onSelectedOptionChange = { value -> selectedOption = value },
            selectedOption = selectedOption ?: ""
        )
        val actionButtonText = if (selectedOption == OPTION_CREATE_NEW) "Create New" else "Save Changes"
        Button(
            onClick = {
                if (selectedOption == OPTION_CREATE_NEW) {
                    // TODO: launch new environment dialog
                } else {
                    coroutineScope.launch {
                        context.environmentSettingsDataStore.updateData { environmentSettings ->
                            val updatedEnvironmentIndex =
                                environmentSettings.environmentsList.indexOfFirst { it.name == selectedOption }
                            environmentSettings
                                .toBuilder()
                                .setActiveEnvironmentIndex(updatedEnvironmentIndex)
                                .build()
                        }
                        onEnvironmentSelected()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(actionButtonText)
        }
    }
}
