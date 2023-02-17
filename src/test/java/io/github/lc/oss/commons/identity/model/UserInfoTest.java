package io.github.lc.oss.commons.identity.model;

import java.util.HashMap;
import java.util.HashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UserInfoTest {
    @Test
    public void test_methods() {
        UserInfo info = new UserInfo();

        info.setDatabaseId("db-id");
        info.setDisplayName("dispaly");
        info.setId("id");
        info.setPermissions(new HashSet<>());
        info.setPreferredTheme("theme");
        info.setUserData(new HashMap<>());
        info.setUsername("user");

        Assertions.assertEquals("db-id", info.getDatabaseId());
        Assertions.assertEquals("dispaly", info.getDisplayName());
        Assertions.assertEquals("id", info.getId());
        Assertions.assertNotNull(info.getPermissions());
        Assertions.assertEquals("theme", info.getPreferredTheme());
        Assertions.assertNotNull(info.getUserData());
        Assertions.assertEquals("user", info.getUsername());
    }
}
