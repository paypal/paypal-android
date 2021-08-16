package com.paypal.android

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.ui.theme.DemoTheme

class DemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemsList = (1..300).map {
            Item("Shoes$it", "$$it.99", R.drawable.ic_baseline_stroller_24)
        }

        setContent {
            DemoTheme {
                ItemList(itemsList = itemsList)
            }
        }
    }
}

data class Item(val name: String, val price: String, val imageResId: Int)

@Composable
fun ItemList(itemsList: List<Item>) {
    LazyColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
        items(itemsList) { item ->
            ItemCard(item = item)
        }
    }
}

@Composable
fun ItemCard(item: Item) {
    DemoTheme {
        Row {
            Image(
                painter = painterResource(id = item.imageResId),
                contentDescription = "Product image",
                modifier = Modifier
                    .size(40.dp)
                    .border(1.5.dp, MaterialTheme.colors.secondary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = item.name,
                    color = MaterialTheme.colors.secondaryVariant,
                    style = MaterialTheme.typography.subtitle2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.price,
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}

@Preview("Light Mode")
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PreviewHomeScreen() {
    ItemCard(item = Item("Shoes", "$9.99", R.drawable.ic_baseline_stroller_24))
}