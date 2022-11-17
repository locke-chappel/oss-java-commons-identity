package com.github.lc.oss.commons.identity.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.lc.oss.commons.api.identity.KeyValue;
import com.github.lc.oss.commons.serialization.Jsonable;

@JsonInclude(Include.NON_EMPTY)
public class UserData implements KeyValue<String>, Jsonable {
    private String key;
    private String value;

    public UserData() {
    }

    @Override
    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
