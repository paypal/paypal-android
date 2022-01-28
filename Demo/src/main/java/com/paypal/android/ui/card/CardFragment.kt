package com.paypal.android.ui.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import com.paypal.android.R
import com.paypal.android.card.CardResult
import com.paypal.android.checkout.PayPalCheckoutResult
import com.paypal.android.ui.theme.DemoTheme
import com.paypal.android.utils.SharedPreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    @Inject
    lateinit var preferenceUtil: SharedPreferenceUtil
    private val cardViewModel: CardViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cardViewModel.environment = preferenceUtil.getEnvironment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DemoTheme {
                    val isLoading by cardViewModel.isLoading.observeAsState(initial = false)
                    ConstraintLayout(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        val (dropDown, fields, button, result) = createRefs()
                        DropDown(
                            cardViewModel.autoFillCards.map { it.first },
                            stringResource(R.string.card_field_prefill_card_fields),
                            { selectedCard -> cardViewModel.onPrefillCardSelected(selectedCard) },
                            modifier = Modifier
                                .padding(16.dp)
                                .constrainAs(dropDown) {
                                    top.linkTo(parent.top)
                                }
                        )
                        CardFields(
                            cardViewModel,
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                .constrainAs(fields) {
                                    top.linkTo(dropDown.bottom, margin = 16.dp)
                                    bottom.linkTo(button.top)
                                },
                        )
                        Button(
                            onClick = { cardViewModel.onCardFieldSubmit() },
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                .fillMaxWidth()
                                .constrainAs(button) {
                                    top.linkTo(fields.bottom, margin = 16.dp)
                                },
                        ) { Text(stringResource(R.string.card_field_submit)) }

                        if (isLoading) {
                            LoadingComposable(modifier = Modifier.constrainAs(result) {
                                top.linkTo(button.bottom)
//                                bottom.linkTo(button.top)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                            })
                        } else {
                            CardResult(
                                cardViewModel.cardResult,
                                modifier = Modifier.constrainAs(result) {
                                    top.linkTo(button.bottom)
//                                    bottom.linkTo(button.top)
                                })
                        }
                    }
//                    Column {
//                        DropDown(
//                            cardViewModel.autoFillCards.map { it.first },
//                            stringResource(R.string.card_field_prefill_card_fields),
//                            { selectedCard -> cardViewModel.onPrefillCardSelected(selectedCard) },
//                            Modifier.padding(16.dp)
//                        )
//                        CardFields(
//                            cardViewModel,
//                            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
//                        )
//                        Button(
//                            onClick = { cardViewModel.onCardFieldSubmit() },
//                            modifier = Modifier
//                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
//                                .fillMaxWidth()
//                        ) { Text(stringResource(R.string.card_field_submit)) }
//                    }
                }
            }
        }
    }

    @Composable
    private fun CardResult(resultLiveData: LiveData<CardResult>, modifier: Modifier) {
        val result by resultLiveData.observeAsState(initial = null)
        when (result) {
            is CardResult.Success -> CardSuccess(
                result as CardResult.Success,
                modifier = modifier
            )
            is CardResult.Error -> CardError(
                result as CardResult.Error,
                modifier = modifier
            )
        }
    }

    @Composable
    private fun CardSuccess(result: CardResult.Success, modifier: Modifier) {
        Column(modifier = modifier.padding(horizontal = 16.dp)) {
            Text(
                text = getString(R.string.order_approved),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = getString(R.string.payer_id, result.orderID),
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = getString(R.string.order_id, result.status),
                fontSize = 16.sp,
                color = Color.Gray,
            )
        }
    }

    @Composable
    private fun CardError(result: CardResult.Error, modifier: Modifier) {
        Column(modifier = modifier.padding(horizontal = 16.dp)) {
            Text(
                text = getString(R.string.order_failed),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = getString(R.string.reason, result.coreSDKError.message),
                fontSize = 16.sp,
                color = Color.Gray,
            )
        }
    }

    @Composable
    private fun LoadingComposable(modifier: Modifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
            Text(text = getString(R.string.creating_order))
        }
    }
}
