package com.github.lc.oss.commons.identity.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.lc.oss.commons.serialization.Jsonable;

@JsonInclude(Include.NON_EMPTY)
public class Token implements com.github.lc.oss.commons.api.identity.TokenResponse, Jsonable {
    private final Long expiration;
    private final String token;

    public Token(@JsonProperty("expiration") Long expiration, @JsonProperty("token") String token) {
        this.expiration = expiration;
        this.token = token;
    }

    @Override
    public Long getExpiration() {
        return this.expiration;
    }

    @Override
    public String getToken() {
        return this.token;
    }
}
