package com.github.lc.oss.commons.identity.model;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.lc.oss.commons.api.identity.ApiObject;
import com.github.lc.oss.commons.api.identity.Messages;

@JsonInclude(Include.NON_EMPTY)
public class ApiResponse<T extends ApiObject> implements com.github.lc.oss.commons.api.identity.ApiResponse<T> {
    private T body;
    private Collection<Messages> messages;

    @Override
    public T getBody() {
        return this.body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    @Override
    public Collection<Messages> getMessages() {
        return this.messages;
    }

    public void setMessages(Collection<Messages> messages) {
        this.messages = messages;
    }
}
