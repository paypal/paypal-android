package com.paypal.android.corepayments.graphql.common

import androidx.annotation.RawRes
import org.json.JSONObject

data class GraphQLRequest(@RawRes val queryResId: Int, val variables: JSONObject)
