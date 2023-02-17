package io.github.lc.oss.commons.identity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.StringEntity;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.identity.model.SignedRequest;
import io.github.lc.oss.commons.serialization.Jsonable;
import io.github.lc.oss.commons.signing.Algorithms;
import io.github.lc.oss.commons.util.CloseableUtil;

class InternalHttpService implements HttpService {
    private JsonService jsonService;
    private StringResponseHandler responseHandler;

    public InternalHttpService(JsonService jsonService) {
        this.jsonService = jsonService;
        this.responseHandler = new StringResponseHandler(jsonService);
    }

    @Override
    public void delete(String url, Map<String, String> headers) {
        HttpDelete request = new HttpDelete(url);
        request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        if (headers != null) {
            headers.forEach((k, v) -> request.setHeader(k, v));
        }

        this.call(request);
    }

    @Override
    public <T> T get(String url, Map<String, String> headers, Class<T> responseType) {
        HttpGet request = new HttpGet(url);
        if (headers != null) {
            headers.forEach((k, v) -> request.setHeader(k, v));
        }

        return this.call(request, responseType);
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

        return this.call(request, responseType);
    }

    @Override
    public void put(String url, Map<String, String> headers, Jsonable requestBody) {
        HttpPut request = new HttpPut(url);
        request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        headers.forEach((k, v) -> request.setHeader(k, v));
        request.setEntity(new StringEntity(this.jsonService.to(requestBody), StandardCharsets.UTF_8));

        this.call(request);
    }

    private void call(HttpUriRequestBase request) {
        this.call(request, null);
    }

    /*
     * Exposed for testing
     */
    @SuppressWarnings("unchecked")
    <T> T call(HttpUriRequestBase request, Class<T> responseType) {
        CloseableHttpClient httpClient = null;
        try {
            httpClient = this.createClient();
            String response = httpClient.execute(request, this.getResponseHandler());

            if (responseType != null && Jsonable.class.isAssignableFrom(responseType)) {
                Class<? extends Jsonable> type = responseType.asSubclass(Jsonable.class);
                return (T) this.jsonService.from(response, type);
            }

            return (T) response;
        } catch (HttpException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new RuntimeException("Error making request", ex);
        } finally {
            CloseableUtil.close(httpClient);
        }
    }

    /*
     * Exposed for testing
     */
    CloseableHttpClient createClient() {
        return HttpClients.createDefault();
    }

    private StringResponseHandler getResponseHandler() {
        return this.responseHandler;
    }
}
