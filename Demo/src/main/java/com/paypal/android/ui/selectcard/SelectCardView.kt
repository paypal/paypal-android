package com.paypal.android.ui.selectcard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paypal.android.models.TestCard
import com.paypal.android.utils.UIConstants

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun SelectCardView(
    viewModel: SelectCardViewModel = viewModel(),
    onSelectedTestCardChange: (String) -> Unit = {}
) {
    LazyColumn(
        verticalArrangement = UIConstants.spacingSmall,
        contentPadding = PaddingValues(UIConstants.paddingMedium),
        modifier = Modifier
            .fillMaxSize()
    ) {
        stickyHeader {
            TestCardGroupHeader("Verified Test Cards")
        }
        items(viewModel.verifiedTestCards) { card ->
            TestCardView(
                testCard = card
            ) {
                onSelectedTestCardChange(card.id)
            }
        }
        stickyHeader {
            TestCardGroupHeader("Test Cards without 3DS")
        }
        items(viewModel.nonThreeDSCards) { card ->
            TestCardView(
                testCard = card
            ) {
                onSelectedTestCardChange(card.id)
            }
        }
        stickyHeader {
            TestCardGroupHeader("Test Cards with 3DS")
        }
        items(viewModel.threeDSCards) { card ->
            TestCardView(
                testCard = card
            ) {
                onSelectedTestCardChange(card.id)
            }
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Preview
@Composable
fun SelectCardViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SelectCardView()
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun TestCardView(testCard: TestCard, onClick: () -> Unit) {
    OutlinedCard(
        onClick = { onClick() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = UIConstants.spacingExtraSmall,
            modifier = Modifier.padding(UIConstants.paddingMedium)
        ) {
            Text(
                text = testCard.name,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = testCard.formattedCardNumber,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
fun TestCardGroupHeader(text: String) {
    Text(
        text = text,
        color = Color.Black,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(top = UIConstants.paddingSmall, bottom = UIConstants.paddingExtraSmall)
    )
}
