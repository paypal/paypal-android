package com.paypal.android.ui.features

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paypal.android.R

// TODO: use a color scheme to keep UI consistent
private val backgroundColor = Color(248, 249, 243)
private val featureTitleColor = Color(20, 29, 47)
private val chevronTint = Color(138, 137, 142)

private val cardFeatures = listOf(
    Feature.CARD_APPROVE_ORDER,
    Feature.CARD_VAULT
)

private val payPalWebFeatures = listOf(
    Feature.PAYPAL_WEB,
    Feature.PAYPAL_BUTTONS
)

private val payPalNativeFeatures = listOf(
    Feature.PAYPAL_NATIVE
)

@ExperimentalFoundationApi
@Composable
fun FeaturesView(
    onFeatureSelected: (Feature) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .background(backgroundColor)
            .padding(horizontal = 16.dp)
    ) {
        stickyHeader {
            FeatureGroupHeader(text = "Card")
        }
        item {
            FeatureOptions(cardFeatures, onFeatureSelected = onFeatureSelected)
        }
        stickyHeader {
            FeatureGroupHeader("PayPal Web")
        }
        item {
            FeatureOptions(payPalWebFeatures, onFeatureSelected = onFeatureSelected)
        }
        stickyHeader {
            FeatureGroupHeader("PayPal Native")
        }
        item {
            FeatureOptions(payPalNativeFeatures, onFeatureSelected = onFeatureSelected)
        }
    }
}

@Composable
fun FeatureOptions(
    features: List<Feature>,
    onFeatureSelected: (Feature) -> Unit,
) {
    Card(
        shape = CardDefaults.elevatedShape,
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        features.forEachIndexed { index, feature ->
            FeatureView(
                feature = feature,
                isLast = (index == features.lastIndex),
                onClick = { onFeatureSelected(feature) }
            )
        }
    }
}

@Composable
fun FeatureGroupHeader(text: String) {
    Spacer(modifier = Modifier.size(24.dp))
    Text(
        text = text,
        color = featureTitleColor,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(modifier = Modifier.size(12.dp))
}

@Composable
fun FeatureView(
    feature: Feature,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val chevronPainter = painterResource(id = R.drawable.chevron)
    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row {
            Text(
                text = stringResource(id = feature.stringRes),
                color = Color.Black,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1.0f)
                    .padding(vertical = 16.dp, horizontal = 20.dp)
            )
            Icon(
                painter = chevronPainter,
                contentDescription = null,
                tint = chevronTint,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.size(20.dp))
        }
        if (!isLast) {
            Divider(
                color = backgroundColor,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
