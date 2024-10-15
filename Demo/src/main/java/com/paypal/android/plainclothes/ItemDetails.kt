package com.paypal.android.plainclothes

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paypal.android.R
import com.paypal.android.utils.UIConstants

@Composable
fun ItemDetails() {
    Image(
        painter = painterResource(id = R.drawable.plain_tshirt),
        contentDescription = stringResource(id = R.string.plain_tshirt_description),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(2.dp, Color.Black)
            .padding(UIConstants.paddingExtraSmall)
    )
    Row {
        Text(
            text = "Amount",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "29.99",
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .weight(1f)
        )
    }
}