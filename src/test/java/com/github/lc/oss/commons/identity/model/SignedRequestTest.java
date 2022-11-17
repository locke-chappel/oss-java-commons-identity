package com.github.lc.oss.commons.identity.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SignedRequestTest {
    @Test
    public void test_methods() {
        SignedRequest sr = new SignedRequest(-1, "app-id", "body-value");

        Assertions.assertEquals(-1, sr.getCreated());
        Assertions.assertEquals("app-id", sr.getApplicationId());
        Assertions.assertEquals("body-value", sr.getBody());
        Assertions.assertEquals("-1app-idbody-value", sr.getSignatureData());
        Assertions.assertNull(sr.getSignature());

        sr.setSignautre("sig");
        Assertions.assertEquals("sig", sr.getSignature());
        Assertions.assertEquals("-1app-idbody-value", sr.getSignatureData());
    }

    @Test
    public void test_methods_nulls() {
        SignedRequest sr = new SignedRequest(-1, null, null);

        Assertions.assertEquals(-1, sr.getCreated());
        Assertions.assertNull(sr.getApplicationId());
        Assertions.assertNull(sr.getBody());
        Assertions.assertEquals("-1nullnull", sr.getSignatureData());
        Assertions.assertNull(sr.getSignature());
    }
}
