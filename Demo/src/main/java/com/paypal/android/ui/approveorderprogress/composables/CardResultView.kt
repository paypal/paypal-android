package com.paypal.android.ui.approveorderprogress.composables

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.cardpayments.model.CardResult

@Composable
fun CardResultView(
    result: CardResult
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Approve Order Result",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Order",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(top = 16.dp)
            )
            Text(
                text = result.orderId,
                modifier = Modifier
                    .padding(top = 4.dp)
            )
            Text(
                text = "Deep Link URL",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(top = 16.dp)
            )
            if (result.deepLinkUrl == null) {
                Text(
                    text = "NOT SET",
                    modifier = Modifier
                        .padding(top = 4.dp)
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(top = 4.dp)
                ) {
                    Text("Scheme:")
                    Text(result.deepLinkUrl?.scheme ?: "NOT SET")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(top = 4.dp)
                ) {
                    Text("Host:")
                    Text(result.deepLinkUrl?.host ?: "NOT SET")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(top = 4.dp)
                ) {
                    Text("Path:")
                    Text(result.deepLinkUrl?.path ?: "NOT SET")
                }
                if (result.deepLinkUrl?.queryParameterNames?.isNotEmpty() == true) {
                    Text(
                        text = "Params:",
                        modifier = Modifier
                            .padding(top = 4.dp)
                    )
                    Column(
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        result.deepLinkUrl?.queryParameterNames?.forEach { paramName ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("$paramName:")
                                Text(
                                    result.deepLinkUrl?.getQueryParameter(paramName)
                                        ?: "PRESENT BUT NOT SET"
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Preview
@Composable
fun CardResultViewWithDeepLinkPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            CardResultView(
                CardResult(
                    "fake-order-id",
                    Uri.parse("fake-scheme://fake-host/fake-path?fakeParam1=value1&fakeParam2=value2")
                )
            )
        }
    }
}

@Preview
@Composable
fun CardResultViewWithoutDeepLinkPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            CardResultView(
                CardResult("fake-order-id")
            )
        }
    }
}
