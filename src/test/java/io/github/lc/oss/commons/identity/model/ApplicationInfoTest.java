package io.github.lc.oss.commons.identity.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationInfoTest {
    @Test
    public void test_methods() {
        ApplicationInfo info = new ApplicationInfo();

        info.setSessionMax(1);
        info.setSessionTimeout(2);

        Assertions.assertEquals(1, info.getSessionMax());
        Assertions.assertEquals(2, info.getSessionTimeout());
    }
}
