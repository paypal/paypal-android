package com.paypal.android.ui.selectcard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paypal.android.models.TestCard

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun SelectCardView(
    viewModel: SelectCardViewModel = viewModel(),
    onTestCardSelected: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        stickyHeader {
            TestCardGroupHeader("Verified Test Cards")
        }
        items(viewModel.verifiedTestCards) { card ->
            Spacer(modifier = Modifier.size(8.dp))
            TestCardView(
                testCard = card
            ) {
                onTestCardSelected(card.id)
            }
        }
        stickyHeader {
            TestCardGroupHeader("Test Cards without 3DS")
        }
        items(viewModel.nonThreeDSCards) { card ->
            Spacer(modifier = Modifier.size(8.dp))
            TestCardView(
                testCard = card
            ) {
                onTestCardSelected(card.id)
            }
        }
        stickyHeader {
            TestCardGroupHeader("Test Cards with 3DS")
        }
        items(viewModel.threeDSCards) { card ->
            Spacer(modifier = Modifier.size(8.dp))
            TestCardView(
                testCard = card
            ) {
                onTestCardSelected(card.id)
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
        Text(
            text = testCard.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
        )
        Text(
            text = testCard.formattedCardNumber,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
        )
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
            .padding(top = 20.dp, bottom = 4.dp)
    )
}
