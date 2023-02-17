package io.github.lc.oss.commons.identity.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_EMPTY)
public class ReissueTokenRequest implements io.github.lc.oss.commons.api.identity.ReissueRequest {
    private final String token;
    private final String applicationId;

    public ReissueTokenRequest( //
            @JsonProperty("token") String token, //
            @JsonProperty("applicationId") String applicationId) //
    {
        this.token = token;
        this.applicationId = applicationId;
    }

    @Override
    public String getToken() {
        return this.token;
    }

    @Override
    public String getApplicationId() {
        return this.applicationId;
    }
}
