package com.paypal.android.uishared.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paypal.android.utils.UIConstants

@Composable
fun UriView(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(UIConstants.paddingSmall),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(UIConstants.paddingSmall),
        ) {
            Text("Scheme:")
            Text(uri.scheme ?: "NOT SET")
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(UIConstants.paddingSmall),
        ) {
            Text("Host:")
            Text(uri.host ?: "NOT SET")
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(UIConstants.paddingSmall),
        ) {
            Text("Path:")
            Text(uri.path ?: "NOT SET")
        }
        val queryParameterNames = uri.queryParameterNames ?: emptySet()
        if (queryParameterNames.isNotEmpty()) {
            Text(text = "Params:")
            Column(
                modifier = Modifier.padding(start = UIConstants.paddingSmall)
            ) {
                for (paramName in queryParameterNames) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(UIConstants.paddingSmall)
                    ) {
                        Text("$paramName:")
                        Text(uri.getQueryParameter(paramName) ?: "PRESENT BUT NOT SET")
                    }
                }
            }
        }
    }
}
