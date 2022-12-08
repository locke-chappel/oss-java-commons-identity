package com.github.lc.oss.commons.identity;

import java.io.IOException;
import java.util.Collection;

import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.github.lc.oss.commons.api.identity.Messages;
import com.github.lc.oss.commons.api.services.JsonService;
import com.github.lc.oss.commons.identity.model.ApiResponse;

public class StringResponseHandler extends AbstractHttpClientResponseHandler<String> {
    private JsonService jsonService;

    public StringResponseHandler(JsonService jsonService) {
        this.jsonService = jsonService;
    }

    @Override
    public String handleEntity(final HttpEntity entity) throws IOException {
        try {
            return EntityUtils.toString(entity);
        } catch (ParseException | IOException ex) {
            throw new IOException("Error reading entity response.", ex);
        }
    }

    @Override
    public String handleResponse(final ClassicHttpResponse response) throws IOException {
        int status = response.getCode();
        switch (status) {
            case HttpStatus.SC_NO_CONTENT:
                return null;
            case HttpStatus.SC_OK:
                break;
            default:
                throw new HttpException("Error making request", status, this.getMessages(response));
        }

        return super.handleResponse(response);
    }

    private Collection<Messages> getMessages(ClassicHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }

        try {
            String json = EntityUtils.toString(entity);
            ApiResponse<?> apiResponse = this.jsonService.from(json, ApiResponse.class);
            if (apiResponse == null) {
                return null;
            }

            return apiResponse.getMessages();
        } catch (ParseException | IOException ex) {
            throw new RuntimeException("Error getting messages", ex);
        }
    }
}
