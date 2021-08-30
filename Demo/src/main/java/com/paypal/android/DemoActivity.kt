package com.paypal.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paypal.android.card.ui.CardFields
import com.paypal.android.ui.theme.DemoTheme

class DemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoTheme {
                Column(horizontalAlignment = Alignment.End) {
                    CardFields()

                    Button(
                        onClick = {

                        },
                        modifier = Modifier.padding(16.dp)
                    ) { Text("Submit") }
                }
            }
        }
    }
}