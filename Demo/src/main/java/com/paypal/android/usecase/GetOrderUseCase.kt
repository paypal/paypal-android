package com.paypal.android.usecase

import com.paypal.android.utils.OrderUtils
import javax.inject.Inject

class GetOrderUseCase @Inject constructor() {

    operator fun invoke() = OrderUtils.createOrderBuilder("100.0")
}
