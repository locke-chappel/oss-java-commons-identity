package io.github.lc.oss.commons.identity.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_EMPTY)
public class SignedRequest implements io.github.lc.oss.commons.api.identity.SignedRequest {
    private long created;
    private final String applicationId;
    private final String body;
    private String signature;

    public SignedRequest( //
            @JsonProperty("created") long created, //
            @JsonProperty("applicationId") String applicationId, //
            @JsonProperty("body") String body //
    ) {
        this.created = created;
        this.applicationId = applicationId;
        this.body = body;
    }

    @Override
    public long getCreated() {
        return this.created;
    }

    @Override
    public String getApplicationId() {
        return this.applicationId;
    }

    @Override
    public String getBody() {
        return this.body;
    }

    @Override
    public String getSignature() {
        return this.signature;
    }

    public void setSignautre(String signature) {
        this.signature = signature;
    }
}
