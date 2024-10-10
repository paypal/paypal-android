package com.paypal.android.ui.features

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.R
import com.paypal.android.utils.UIConstants

private val cardFeatures = listOf(
    Feature.CARD_APPROVE_ORDER,
    Feature.CARD_VAULT
)

private val payPalWebFeatures = listOf(
    Feature.PAYPAL_WEB,
    Feature.PAYPAL_BUTTONS,
    Feature.PAYPAL_STATIC_BUTTONS,
    Feature.PAYPAL_WEB_VAULT
)

@ExperimentalFoundationApi
@Composable
fun FeaturesView(
    onSelectedFeatureChange: (Feature) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = UIConstants.paddingMedium)
            .fillMaxSize()
    ) {
        stickyHeader {
            FeatureGroupHeader(text = "Card")
        }
        item {
            FeatureOptions(cardFeatures, onSelectedFeatureChange = onSelectedFeatureChange)
        }
        stickyHeader {
            FeatureGroupHeader("PayPal Web")
        }
        item {
            FeatureOptions(payPalWebFeatures, onSelectedFeatureChange = onSelectedFeatureChange)
        }
    }
}

@Composable
fun FeatureOptions(
    features: List<Feature>,
    onSelectedFeatureChange: (Feature) -> Unit,
) {
    Card(
        shape = CardDefaults.elevatedShape,
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        features.forEachIndexed { index, feature ->
            FeatureView(
                feature = feature,
                isLast = (index == features.lastIndex),
                onClick = { onSelectedFeatureChange(feature) }
            )
        }
    }
}

@Composable
fun FeatureGroupHeader(text: String) {
    Spacer(modifier = Modifier.size(UIConstants.paddingLarge))
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(modifier = Modifier.size(UIConstants.paddingMedium))
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
    ) {
        Row {
            Text(
                text = stringResource(id = feature.stringRes),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1.0f)
                    .padding(UIConstants.paddingMedium)
            )
            Icon(
                painter = chevronPainter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(UIConstants.chevronSize)
                    .align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.size(UIConstants.paddingMedium))
        }
        if (!isLast) {
            Divider(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(start = UIConstants.paddingMedium)
            )
        }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun FeaturesViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            FeaturesView(onSelectedFeatureChange = {})
        }
    }
}
