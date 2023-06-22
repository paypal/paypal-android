package com.paypal.android.ui.approveorderprogress.composables

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UriView(uri: Uri) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(top = 4.dp)
    ) {
        Text("Scheme:")
        Text(uri.scheme ?: "NOT SET")
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(top = 4.dp)
    ) {
        Text("Host:")
        Text(uri.host ?: "NOT SET")
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(top = 4.dp)
    ) {
        Text("Path:")
        Text(uri.path ?: "NOT SET")
    }
    if (uri.queryParameterNames?.isNotEmpty() == true) {
        Text(
            text = "Params:",
            modifier = Modifier
                .padding(top = 4.dp)
        )
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            uri.queryParameterNames?.forEach { paramName ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("$paramName:")
                    Text(uri.getQueryParameter(paramName) ?: "PRESENT BUT NOT SET")
                }
            }
        }
    }
}
