package io.github.lc.oss.commons.identity.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class ApplicationInfo implements io.github.lc.oss.commons.api.identity.ApplicationInfo {
    private long sessionTimeout;
    private long sessionMax;

    @Override
    public long getSessionTimeout() {
        return this.sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    @Override
    public long getSessionMax() {
        return this.sessionMax;
    }

    public void setSessionMax(long sessionMax) {
        this.sessionMax = sessionMax;
    }
}
