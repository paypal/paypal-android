package com.paypal.android.uishared.components

import android.net.Uri
import androidx.compose.foundation.layout.Column
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
import com.paypal.android.cardpayments.CardResult
import com.paypal.android.utils.UIConstants

@Composable
fun CardResultView(result: CardResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UIConstants.paddingMedium)
    ) {
        Text(
            text = "Order ID",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(top = UIConstants.paddingMedium)
        )
        Text(
            text = result.orderId,
            modifier = Modifier
                .padding(top = UIConstants.paddingExtraSmall)
        )
        Text(
            text = "Deep Link URL",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(top = UIConstants.paddingMedium)
        )
        if (result.deepLinkUrl == null) {
            Text(
                text = "NOT SET",
                modifier = Modifier
                    .padding(top = UIConstants.paddingExtraSmall)
            )
        } else {
            UriView(uri = result.deepLinkUrl!!)
        }
        Spacer(modifier = Modifier.size(UIConstants.paddingLarge))
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
