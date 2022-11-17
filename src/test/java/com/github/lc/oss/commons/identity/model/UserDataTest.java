package com.github.lc.oss.commons.identity.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UserDataTest {
    @Test
    public void test_methods() {
        UserData data = new UserData();

        data.setKey("k");
        data.setValue("v");

        Assertions.assertEquals("k", data.getKey());
        Assertions.assertEquals("v", data.getValue());
    }
}
