package com.paypal.android.checkout

import com.paypal.pyplcheckout.pojo.Address
import com.paypal.pyplcheckout.pojo.ApprovePayment
import com.paypal.pyplcheckout.pojo.ApprovePaymentData
import com.paypal.pyplcheckout.pojo.ApprovePaymentResponse
import com.paypal.pyplcheckout.pojo.Buyer
import com.paypal.pyplcheckout.pojo.Cart
import com.paypal.pyplcheckout.pojo.Email
import com.paypal.pyplcheckout.pojo.Name
import com.paypal.pyplcheckout.pojo.PaymentContingencies
import com.paypal.pyplcheckout.pojo.Phone
import org.junit.Test

import org.junit.Assert.*
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ApprovalDataTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }


    private fun generateApprovePaymentResponse(data: ApprovePaymentData? = null): ApprovePaymentResponse {
        return ApprovePaymentResponse(
            data = data,
            extensions = null,
            errors = null
        )
    }

    private fun generateApprovePaymentData(approvePayment: ApprovePayment? = null): ApprovePaymentData {
        return ApprovePaymentData(
            approvePayment = approvePayment
        )
    }

    private fun generateApprovePayment(
        paymentContingencies: PaymentContingencies? = null,
        cart: Cart? = null,
        buyer: Buyer? = null
    ): ApprovePayment {
        return ApprovePayment(
            paymentContingencies = paymentContingencies,
            cart = cart,
            buyer = buyer
        )
    }

    private fun generateBuyer(
        userId: String? = null,
        email: Email? = null,
        name: Name? = null,
        addresses: List<Address>? = null,
        phone: List<Phone>? = null
    ): Buyer = Buyer(userId = userId, email = email, name = name, addresses = addresses, phones = phone)

    private fun generateCart(
        cartId: String? = null,
        paymentId: String? = null
    ): Cart {
        return Cart(
            cartId = cartId,
            paymentId = paymentId
        )
    }

}