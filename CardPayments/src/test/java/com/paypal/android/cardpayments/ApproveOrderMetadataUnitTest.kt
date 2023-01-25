package com.paypal.android.cardpayments

import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class ApproveOrderMetadataUnitTest {

    @Test
    fun `it should be able to serialize and deserialize itself in JSON`() {

        // language=JSON
        val jsonString = """
            {
                "order_id": "sample-order-id",
                "payment_source": {
                  "last_digits": "4111",
                  "brand": "Visa",
                  "type": "sample-type",
                  "authentication_result": {
                    "liability_shift": "YES",
                    "three_d_secure": {
                      "enrollment_status": "ENROLLED",
                      "authentication_status": "AUTHENTICATED"
                    }
                  }
                }
            }
        """

        val originalJSON = JSONObject(jsonString)
        val reserializedJSON = ApproveOrderMetadata.fromJSON(originalJSON)?.toJSON()

        JSONAssert.assertEquals(originalJSON, reserializedJSON, true)
    }
}
