package io.github.lc.oss.commons.identity;

import java.util.Collection;

import io.github.lc.oss.commons.api.identity.Messages;

public class HttpException extends RuntimeException {
    private static final long serialVersionUID = -546659468661205613L;

    private final Collection<Messages> messages;
    private final int status;

    public HttpException(String message, int status, Collection<Messages> messages) {
        super(message);
        this.status = status;
        this.messages = messages;
    }

    public Collection<Messages> getMessages() {
        return this.messages;
    }

    public int getStatus() {
        return this.status;
    }
}
