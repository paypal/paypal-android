package com.paypal.android.ui.paypal

import com.paypal.checkout.createorder.CurrencyCode
import com.paypal.checkout.createorder.ItemCategory
import com.paypal.checkout.createorder.OrderIntent
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
import com.paypal.checkout.order.Order
import com.paypal.checkout.order.PurchaseUnit
import com.paypal.checkout.order.Shipping
import com.paypal.checkout.order.UnitAmount

object OrderUtils {

    val orderWithShipping = """
        {
        	"application_context": {
        		"brand_name": "EXAMPLE INC",
        		"cancel_url": "https://example.com/cancel",
        		"landing_page": "BILLING",
        		"locale": "de-DE",
        		"return_url": "https://example.com/return",
        		"shipping_preference": "GET_FROM_FILE",
        		"user_action": "PAY_NOW"
        	},
        	"intent": "AUTHORIZE",
        	"purchase_units": [{
        			"amount": {
        				"breakdown": {
        					"discount": {
        						"currency_code": "USD",
        						"value": "00.00"
        					},
        					"handling": {
        						"currency_code": "USD",
        						"value": "00.00"
        					},
        					"item_total": {
        						"currency_code": "USD",
        						"value": "00.00"
        					},
        					"shipping": {
        						"currency_code": "USD",
        						"value": "0.00"
        					},
        					"shipping_discount": {
        						"currency_code": "USD",
        						"value": "00.00"
        					},
        					"tax_total": {
        						"currency_code": "USD",
        						"value": "100"
        					}
        				},
        				"currency_code": "USD",
        				"value": "100.00"
        			},
        			"custom_id": "CUST-HighFashions",
        			"description": "Sporting Goods",
        			"items": [{
        					"category": "PHYSICAL_GOODS",
        					"description": "Green XL",
        					"name": "T-Shirt",
        					"quantity": "1",
        					"sku": "sku01",
        					"tax": {
        						"currency_code": "USD",
        						"value": "00.00"
        					},
        					"unit_amount": {
        						"currency_code": "USD",
        						"value": "00.00"
        					}
        				},{
        					"category": "PHYSICAL_GOODS",
        					"description": "Running, Size 10.5",
        					"name": "Shoes",
        					"quantity": "2",
        					"sku": "sku02",
        					"tax": {
        						"currency_code": "USD",
        						"value": "00.00"
        					},
        					"unit_amount": {
        						"currency_code": "USD",
        						"value": "00.00"
        					}
        				}
        			],
        			"payee": {
        				"email_address": "merchant@email.com",
        				"merchant_id": "X5XAHHCG636FA"
        			},
        			"reference_id": "PUHF",
        			"shipping": {
        				"address": {
        					"address_line_1": "123 Townsend St",
        					"address_line_2": "Floor 6",
        					"admin_area_1": "CA",
        					"admin_area_2": "San Francisco",
        					"country_code": "US",
        					"postal_code": "94107"
        				},
        				"options": [{
        						"amount": {
        							"currency_code": "USD",
        							"value": "10.00"
        						},
        						"id": "1",
        						"label": "Standard Shipping",
        						"selected": false,
        						"type": "SHIPPING"
        					},{
        						"amount": {
        							"currency_code": "USD",
        							"value": "9.00"
        						},
        						"id": "2",
        						"label": "2 days Shipping",
        						"selected": false,
        						"type": "SHIPPING"
        					},{
        						"amount": {
        							"currency_code": "USD",
        							"value": "8.00"
        						},
        						"id": "3",
        						"label": "5 days Shipping",
        						"selected": false,
        						"type": "SHIPPING"
        					},{
        						"amount": {
        							"currency_code": "USD",
        							"value": "7.00"
        						},
        						"id": "4",
        						"label": "Express Shipping",
        						"selected": false,
        						"type": "SHIPPING"
        					},{
        						"amount": {
        							"currency_code": "USD",
        							"value": "00.00"
        						},
        						"id": "5",
        						"label": "1 day Shipping",
        						"selected": true,
        						"type": "SHIPPING"
        					},{
        						"amount": {
        							"currency_code": "USD",
        							"value": "5.00"
        						},
        						"id": "6",
        						"label": "Pick up from 1122 N 1st st, San Jose CA, 95129",
        						"selected": false,
        						"type": "PICKUP"
        					},{
        						"amount": {
        							"currency_code": "USD",
        							"value": "4.00"
        						},
        						"id": "8",
        						"label": "Pick up from 999 N 1st st, San Fransisco CA, 95009",
        						"selected": false,
        						"type": "PICKUP"
        					},{
        						"amount": {
        							"currency_code": "USD",
        							"value": "3.00"
        						},
        						"id": "9",
        						"label": "In store pickup",
        						"selected": false,
        						"type": "PICKUP"
        					},{
        						"amount": {
        							"currency_code": "USD",
        							"value": "2.00"
        						},
        						"id": "10",
        						"label": "Pick up from Amazon",
        						"selected": false,
        						"type": "PICKUP"
        					},{
        						"amount": {
        							"currency_code": "USD",
        							"value": "1.00"
        						},
        						"id": "11",
        						"label": "Pick up from Tesla warehouse",
        						"selected": false,
        						"type": "PICKUP"
        					}
        				]
        			},
        			"soft_descriptor": "HighFashions"
        		}
        	]
        }
    """.trimIndent()

    fun createOrderBuilder(
        value: String = "0.01",
        shippingPreference: ShippingPreference = ShippingPreference.GET_FROM_FILE,
        userAction: UserAction = UserAction.CONTINUE,
        currency: CurrencyCode = CurrencyCode.USD,
        orderIntent: OrderIntent = OrderIntent.CAPTURE,
        processingInstruction: ProcessingInstruction? = null
    ): Order {

        return Order.Builder()
            .intent(orderIntent)
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
                            .value("00.00")
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
                            .value(value)
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

    private fun createShippingOptionsBuilder(
        currency: CurrencyCode,
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
            .build()

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
            .build()

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
            .build()

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
            .build()

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
            .build()

        val pickUpOption7 = Options.Builder()
            .id("8")
            .selected(false)
            .label("Pick up from 999 N 1st st, San Fransisco CA, 95009")
            .type(ShippingType.PICKUP)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("4.00")
                    .build()
            )
            .build()

        val pickUpOption8 = Options.Builder()
            .id("9")
            .selected(false)
            .label("In store pickup")
            .type(ShippingType.PICKUP)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("3.00")
                    .build()
            )
            .build()

        val pickUpOption9 = Options.Builder()
            .id("10")
            .selected(false)
            .label("Pick up from Amazon")
            .type(ShippingType.PICKUP)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("2.00")
                    .build()
            )
            .build()

        val pickUpOption10 = Options.Builder()
            .id("11")
            .selected(false)
            .label("Pick up from Tesla warehouse")
            .type(ShippingType.PICKUP)
            .amount(
                UnitAmount.Builder()
                    .currencyCode(currency)
                    .value("1.00")
                    .build()
            )
            .build()

        val selectedShippingOption5 = Options.Builder()
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
            .build()

        return listOf(
            shippingOption1,
            shippingOption2,
            shippingOption3,
            shippingOption4,
            selectedShippingOption5,
            pickUpOption6,
            pickUpOption7,
            pickUpOption8,
            pickUpOption9,
            pickUpOption10
        )
    }

    fun Float.asValueString(): String = "%.2f".format(this)
}
