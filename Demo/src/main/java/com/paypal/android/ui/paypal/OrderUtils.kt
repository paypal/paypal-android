package com.paypal.android.ui.paypal

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
}
