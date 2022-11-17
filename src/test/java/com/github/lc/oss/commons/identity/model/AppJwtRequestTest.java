package com.github.lc.oss.commons.identity.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AppJwtRequestTest {
    @Test
    public void test_members_nulls() {
        AppJwtRequest ajr = new AppJwtRequest(-1, null);

        Assertions.assertEquals(-1, ajr.getTtl());
        Assertions.assertNull(ajr.getPermissions());
    }
}
