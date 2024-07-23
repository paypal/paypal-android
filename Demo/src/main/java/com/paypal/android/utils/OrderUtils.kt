package com.paypal.android.utils

import com.paypal.android.corepayments.OrderIntent
import com.paypal.checkout.createorder.CurrencyCode
import com.paypal.checkout.createorder.ItemCategory
import com.paypal.checkout.createorder.ProcessingInstruction
import com.paypal.checkout.createorder.ShippingPreference
import com.paypal.checkout.createorder.ShippingType
import com.paypal.checkout.createorder.UserAction
import com.paypal.checkout.order.Address
import com.paypal.checkout.order.Amount
import com.paypal.checkout.order.AppContext
import com.paypal.checkout.order.BreakDown
import com.paypal.checkout.order.Items
import com.paypal.checkout.order.Options
import com.paypal.checkout.order.OrderRequest
import com.paypal.checkout.order.PurchaseUnit
import com.paypal.checkout.order.Shipping
import com.paypal.checkout.order.UnitAmount

@SuppressWarnings("LongParameterList", "LongMethod")
object OrderUtils {

    fun createOrderBuilder(
        value: String = "0.01",
        shippingPreference: ShippingPreference = ShippingPreference.GET_FROM_FILE,
        userAction: UserAction = UserAction.CONTINUE,
        currency: CurrencyCode = CurrencyCode.USD,
        orderIntent: OrderIntent = OrderIntent.CAPTURE,
        processingInstruction: ProcessingInstruction? = null
    ): OrderRequest {

        val mappedOrderIntent = when (orderIntent) {
            OrderIntent.CAPTURE -> com.paypal.checkout.createorder.OrderIntent.CAPTURE
            OrderIntent.AUTHORIZE -> com.paypal.checkout.createorder.OrderIntent.AUTHORIZE
        }
        return OrderRequest.Builder()
            .intent(mappedOrderIntent)
            .processingInstruction(processingInstruction)
            .appContext(
                AppContext.Builder()
                    .returnUrl("https://example.com/return")
                    .cancelUrl("https://example.com/cancel")
                    .brandName("EXAMPLE INC")
                    .locale("de-DE")
                    .landingPage("BILLING")
                    .shippingPreference(shippingPreference)
                    .userAction(userAction)
                    .build()
            )
            .purchaseUnitList(
                arrayListOf(
                    PurchaseUnit.Builder()
                        .referenceId("PUHF")
                        .description("Sporting Goods")
                        .customId("CUST-HighFashions")
                        .softDescriptor("HighFashions")
                        .amount(getAmount(currency, value))
                        .items(createItemsBuilder(currency))
                        .shipping(
                            Shipping.Builder()
                                .address(
                                    Address.Builder()
                                        .addressLine1("123 Townsend St")
                                        .addressLine2("Floor 6")
                                        .adminArea1("CA")
                                        .adminArea2("San Francisco")
                                        .postalCode("94107")
                                        .countryCode("US")
                                        .build()
                                )
                                .options(
                                    if (shippingPreference == ShippingPreference.GET_FROM_FILE) {
                                        createShippingOptionsBuilder(
                                            currency,
                                        )
                                    } else {
                                        null
                                    }
                                )
                                .build()
                        )
                        .build()
                )
            )
            .build()
    }

    fun getAmount(
        currency: CurrencyCode = CurrencyCode.USD,
        value: String = "0.01",
        shippingValue: String = "0.00"
    ): Amount {
        val valueAsFloat = value.toFloat()
        val shippingValueAsFloat = shippingValue.toFloat()
        val totalValue = valueAsFloat + shippingValueAsFloat

        return Amount.Builder()
            .currencyCode(currency)
            .value(totalValue.asValueString())
            .breakdown(
                BreakDown.Builder()
                    .itemTotal(
                        UnitAmount.Builder()
                            .currencyCode(currency)
                            .value(value)
                            .build()
                    )
                    .shipping(
                        UnitAmount.Builder()
                            .currencyCode(currency)
                            .value(shippingValueAsFloat.asValueString())
                            .build()
                    )
                    .handling(
                        UnitAmount.Builder()
                            .currencyCode(currency)
                            .value("00.00")
                            .build()
                    )
                    .taxTotal(
                        UnitAmount.Builder()
                            .currencyCode(currency)
                            .value("00.00")
                            .build()
                    )
                    .shippingDiscount(
                        UnitAmount.Builder()
                            .currencyCode(currency)
                            .value("00.00")
                            .build()
                    )
                    .discount(
                        UnitAmount.Builder()
                            .currencyCode(currency)
                            .value("00.00")
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun createItemsBuilder(currency: CurrencyCode): List<Items> {
        val item1 = Items.Builder()
            .name("T-Shirt")
            .description("Green XL")
            .sku("sku01")
            .unitAmount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("00.00")
                    .build()
            )
            .tax(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("00.00")
                    .build()
            )
            .quantity("1")
            .category(ItemCategory.PHYSICAL_GOODS)
            .build()

        val item2 = Items.Builder()
            .name("Shoes")
            .description("Running, Size 10.5")
            .sku("sku02")
            .unitAmount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("00.00")
                    .build()
            )
            .tax(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("00.00")
                    .build()
            )
            .quantity("2")
            .category(ItemCategory.PHYSICAL_GOODS)
            .build()

        return listOf(item1, item2)
    }

    fun createShippingOptionsBuilder(
        currency: CurrencyCode = CurrencyCode.USD,
        selectedId: String = "5"
    ): List<Options> {
        val shippingOption1 = Options.Builder()
            .id("1")
            .selected(false)
            .label("Standard Shipping")
            .type(ShippingType.SHIPPING)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("10.00")
                    .build()
            )

        val shippingOption2 = Options.Builder()
            .id("2")
            .selected(false)
            .label("2 days Shipping")
            .type(ShippingType.SHIPPING)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("9.00")
                    .build()
            )

        val shippingOption3 = Options.Builder()
            .id("3")
            .selected(false)
            .label("5 days Shipping")
            .type(ShippingType.SHIPPING)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("8.00")
                    .build()
            )

        val shippingOption4 = Options.Builder()
            .id("4")
            .selected(false)
            .label("Express Shipping")
            .type(ShippingType.SHIPPING)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("7.00")
                    .build()
            )

        val shippingOption5 = Options.Builder()
            .id("5")
            .selected(true)
            .label("1 day Shipping")
            .type(ShippingType.SHIPPING)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("00.00")
                    .build()
            )

        val pickUpOption6 = Options.Builder()
            .id("6")
            .selected(false)
            .label("Pick up from 1122 N 1st st, San Jose CA, 95129")
            .type(ShippingType.PICKUP)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("5.00")
                    .build()
            )

        val pickUpOption7 = Options.Builder()
            .id("7")
            .selected(false)
            .label("Pick up from 999 N 1st st, San Fransisco CA, 95009")
            .type(ShippingType.PICKUP)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("4.00")
                    .build()
            )

        val pickUpOption8 = Options.Builder()
            .id("8")
            .selected(false)
            .label("In store pickup")
            .type(ShippingType.PICKUP)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("3.00")
                    .build()
            )

        val pickUpOption9 = Options.Builder()
            .id("9")
            .selected(false)
            .label("Pick up from Amazon")
            .type(ShippingType.PICKUP)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("2.00")
                    .build()
            )

        val pickUpOption10 = Options.Builder()
            .id("10")
            .selected(false)
            .label("Pick up from Tesla warehouse")
            .type(ShippingType.PICKUP)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("1.00")
                    .build()
            )

        val options = listOf(
            shippingOption1,
            shippingOption2,
            shippingOption3,
            shippingOption4,
            shippingOption5,
            pickUpOption6,
            pickUpOption7,
            pickUpOption8,
            pickUpOption9,
            pickUpOption10
        )
        return options.mapIndexed { index, builder ->
            val id = "${index + 1}"
            builder.id(id).selected(selectedId == id).build()
        }
    }

    fun Float.asValueString(): String = "%.2f".format(this)
}
