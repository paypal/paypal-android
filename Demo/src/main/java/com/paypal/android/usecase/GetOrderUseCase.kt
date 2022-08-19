package com.paypal.android.usecase

import com.paypal.android.ui.paypal.OrderUtils
import javax.inject.Inject

class GetOrderUseCase @Inject constructor() {

    operator fun invoke() = OrderUtils.createOrderBuilder("100.0")
}
