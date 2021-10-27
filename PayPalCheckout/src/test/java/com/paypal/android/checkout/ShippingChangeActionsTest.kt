package com.paypal.android.checkout

import com.paypal.android.checkout.patch.PatchOrderRequest
import com.paypal.android.checkout.shipping.OnPatchComplete
import com.paypal.android.checkout.shipping.ShippingChangeActions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class ShippingChangeActionsTest {

    private val patchOrderRequest: PatchOrderRequest = mockk(relaxed = true)
    private val onPatchComplete: OnPatchComplete = mockk(relaxed = true)
    private val shippingActions: com.paypal.checkout.shipping.ShippingChangeActions =
        mockk(relaxed = true)

    private val subject = ShippingChangeActions(shippingActions)

    @Test
    fun `when patchOrder is invoked with OnPatchComplete, nested patchOrder is called and callback invoked`() {
        val patchCompleteSlot = slot<com.paypal.checkout.order.actions.OnPatchComplete>()
        every {
            shippingActions.patchOrder(
                patchOrderRequest.toNativeCheckout,
                capture(patchCompleteSlot)
            )
        } answers { patchCompleteSlot.captured.onPatchComplete() }
        subject.patchOrder(patchOrderRequest, onPatchComplete)
        verify {
            shippingActions.patchOrder(
                patchOrderRequest.toNativeCheckout,
                any<com.paypal.checkout.order.actions.OnPatchComplete>()
            )
            onPatchComplete.onPatchComplete()
        }
    }

    @Test
    fun `when patchOrder is invoked, nested patchOrder is called`() {
        val onComplete = {}
        subject.patchOrder(patchOrderRequest, onComplete = onComplete)
        verify {
            shippingActions.patchOrder(
                patchOrderRequest.toNativeCheckout,
                onComplete
            )
        }
    }

    @Test
    fun `when reject is invoked, nested reject is called`() {
        subject.reject()
        verify {
            shippingActions.reject()
        }
    }
}
