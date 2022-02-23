package com.paypal.android.card;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.paypal.android.core.CoreConfig;
import com.paypal.android.core.PayPalSDKError;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class SuspendFunctionTest {

    @Test
    public void useAppContext() {
        CoreConfig config = new CoreConfig("");
        CardClient client = new CardClient(config);
        Card card = new Card("","","");
        CardRequest cardRequest = new CardRequest("", card);


        client.approveOrder(cardRequest, new ApproveOrderCallback() {
            @Override
            public void success(@NonNull CardResult result) {

            }

            @Override
            public void failure(@NonNull PayPalSDKError error) {

            }
        });
    }
}
