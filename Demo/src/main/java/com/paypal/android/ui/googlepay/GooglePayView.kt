package com.paypal.android.ui.googlepay

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun GooglePayView() {
    Text("Helloo")
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