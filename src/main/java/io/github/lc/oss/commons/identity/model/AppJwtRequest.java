package io.github.lc.oss.commons.identity.model;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_EMPTY)
public class AppJwtRequest implements io.github.lc.oss.commons.api.identity.AppJwtRequest {
    private final long ttl;
    private final Map<String, Collection<String>> permissions;

    public AppJwtRequest(@JsonProperty("ttl") long ttl, @JsonProperty("permissions") Map<String, Collection<String>> permissions) {
        this.ttl = ttl;
        this.permissions = permissions;
    }

    @Override
    public long getTtl() {
        return this.ttl;
    }

    @Override
    public Map<String, Collection<String>> getPermissions() {
        return this.permissions;
    }
}
