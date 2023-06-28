package com.paypal.android.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun stringResourceListOf(@StringRes vararg resources: Int): List<String> =
    resources.map { stringResource(id = it) }
