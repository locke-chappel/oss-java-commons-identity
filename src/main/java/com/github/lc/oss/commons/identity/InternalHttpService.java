package com.github.lc.oss.commons.identity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.github.lc.oss.commons.api.identity.Messages;
import com.github.lc.oss.commons.api.services.JsonService;
import com.github.lc.oss.commons.identity.model.ApiResponse;
import com.github.lc.oss.commons.identity.model.SignedRequest;
import com.github.lc.oss.commons.serialization.Jsonable;
import com.github.lc.oss.commons.signing.Algorithms;
import com.github.lc.oss.commons.util.CloseableUtil;

class InternalHttpService implements HttpService {
    private JsonService jsonService;

    public InternalHttpService(JsonService jsonService) {
        this.jsonService = jsonService;
    }

    @Override
    public void delete(String url, Map<String, String> headers) {
        HttpDelete request = new HttpDelete(url);
        request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        if (headers != null) {
            headers.forEach((k, v) -> request.setHeader(k, v));
        }

        this.call(request, HttpStatus.SC_NO_CONTENT);
    }

    @Override
    public <T> T get(String url, Map<String, String> headers, Class<T> responseType) {
        HttpGet request = new HttpGet(url);
        if (headers != null) {
            headers.forEach((k, v) -> request.setHeader(k, v));
        }

        return this.call(request, responseType, HttpStatus.SC_OK);
    }

    @Override
    public <T> T post(String url, Map<String, String> headers, Class<T> responseType, Jsonable requestBody) {
        return this.post(url, headers, responseType, requestBody, null);
    }

    @Override
    public <T> T post(String url, Map<String, String> headers, Class<T> responseType, Jsonable requestBody, String privateKey) {
        if (requestBody instanceof SignedRequest) {
            SignedRequest signed = (SignedRequest) requestBody;
            String signature = Algorithms.ED448.getSignature(privateKey, signed.getSignatureData());
            signed.setSignautre(signature);
        }

        HttpPost request = new HttpPost(url);
        request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        headers.forEach((k, v) -> request.setHeader(k, v));
        request.setEntity(new StringEntity(this.jsonService.to(requestBody), StandardCharsets.UTF_8));

        return this.call(request, responseType, HttpStatus.SC_OK);
    }

    @Override
    public void put(String url, Map<String, String> headers, Jsonable requestBody) {
        HttpPut request = new HttpPut(url);
        request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        headers.forEach((k, v) -> request.setHeader(k, v));
        request.setEntity(new StringEntity(this.jsonService.to(requestBody), StandardCharsets.UTF_8));

        this.call(request, HttpStatus.SC_NO_CONTENT);
    }

    private void call(HttpRequestBase request, int expectedStatus) {
        this.call(request, null, expectedStatus);
    }

    /*
     * Exposed for testing
     */
    @SuppressWarnings("unchecked")
    <T> T call(HttpRequestBase request, Class<T> responseType, int expectedStatus) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = this.createClient();
            response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != expectedStatus) {
                Collection<Messages> messages = this.getMessages(response);
                throw new HttpException("Error making request", response.getStatusLine().getStatusCode(), messages);
            }

            if (responseType != null) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String string = EntityUtils.toString(entity);
                    if (Jsonable.class.isAssignableFrom(responseType)) {
                        Class<? extends Jsonable> type = responseType.asSubclass(Jsonable.class);
                        return (T) this.jsonService.from(string, type);
                    }

                    return (T) string;
                }
            }

            return null;
        } catch (HttpException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new RuntimeException("Error making request", ex);
        } finally {
            CloseableUtil.close(httpClient);
            CloseableUtil.close(response);
        }
    }

    private Collection<Messages> getMessages(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        String json = EntityUtils.toString(entity);
        ApiResponse<?> apiResponse = this.jsonService.from(json, ApiResponse.class);

        if (apiResponse == null) {
            return null;
        }

        return apiResponse.getMessages();
    }

    /*
     * Exposed for testing
     */
    CloseableHttpClient createClient() {
        return HttpClients.createDefault();
    }
}
