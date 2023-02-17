package io.github.lc.oss.commons.identity.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReissueTokenRequestTest {
    @Test
    public void test_methods() {
        ReissueTokenRequest request = new ReissueTokenRequest("token", "app-id");

        Assertions.assertEquals("token", request.getToken());
        Assertions.assertEquals("app-id", request.getApplicationId());
    }
}
