package io.github.lc.oss.commons.identity;

import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.identity.model.SignedRequest;
import io.github.lc.oss.commons.identity.model.UserData;
import io.github.lc.oss.commons.identity.model.UserInfo;
import io.github.lc.oss.commons.identity.model.UserInfoResponse;
import io.github.lc.oss.commons.signing.Algorithms;
import io.github.lc.oss.commons.signing.KeyGenerator;
import io.github.lc.oss.commons.testing.AbstractMockTest;

@SuppressWarnings("unchecked")
public class InternalHttpServiceTest extends AbstractMockTest {
    @Mock
    private JsonService jsonService;
    @Mock
    private CloseableHttpClient httpClient;

    private KeyGenerator keyGenerator = new KeyGenerator();

    private InternalHttpService service;

    @BeforeEach
    public void setup() {
        this.service = new InternalHttpService(this.jsonService) {
            @Override
            protected CloseableHttpClient createClient() {
                return InternalHttpServiceTest.this.httpClient;
            }
        };
    }

    @Test
    public void test_coverageFiller() {
        InternalHttpService service = new InternalHttpService(this.jsonService);
        Assertions.assertNotNull(service.createClient());
    }

    @Test
    public void test_delete() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-header", "value");

        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenReturn(null);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        this.service.delete("localhost", headers);
    }

    @Test
    public void test_delete_error() {
        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenThrow(new IOException("BOOM!"));
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        try {
            this.service.delete("localhost", null);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error making request", ex.getMessage());
            Assertions.assertTrue(!(ex instanceof HttpException));
            Assertions.assertNotNull(ex.getCause());
            Assertions.assertEquals("BOOM!", ex.getCause().getMessage());
        }
    }

    @Test
    public void test_get() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-header", "junk");

        final String responseJson = "{}";
        UserInfoResponse<UserInfo> uiResponse = new UserInfoResponse<>();

        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenReturn(responseJson);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        Mockito.when(this.jsonService.from(responseJson, UserInfoResponse.class)).thenReturn(uiResponse);

        UserInfoResponse<UserInfo> result = this.service.get("localhost", headers, UserInfoResponse.class);
        Assertions.assertNotNull(result);
    }

    @Test
    public void test_get_string() {
        final String responseJson = "text";

        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenReturn(responseJson);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        String result = this.service.get("localhost", null, String.class);
        Assertions.assertEquals("text", result);
    }

    @Test
    public void test_get_string_null() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-header", "junk");

        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenReturn(null);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        String result = this.service.get("localhost", headers, String.class);
        Assertions.assertNull(result);
    }

    @Test
    public void test_get_closeException() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-header", "junk");

        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenReturn(null);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        try {
            Mockito.doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    throw new RuntimeException("BOOM!");
                }

            }).when(this.httpClient).close();
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        try {
            this.service.get("localhost", headers, String.class);
            Assertions.fail("Expectex exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("BOOM!", ex.getMessage());
            Assertions.assertNull(ex.getCause());
        }
    }

    @Test
    public void test_get_error_httpStatus() {
        Map<String, String> headers = new HashMap<>();

        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenThrow(new HttpException("Error making request", HttpStatus.SC_UNPROCESSABLE_ENTITY, null));
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        try {
            this.service.get("localhost", headers, UserInfoResponse.class);
            Assertions.fail("Expectex exception");
        } catch (HttpException ex) {
            Assertions.assertEquals("Error making request", ex.getMessage());
            Assertions.assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, ex.getStatus());
            Assertions.assertNull(ex.getMessages());
        }
    }

    @Test
    public void test_get_error_exception() {
        Map<String, String> headers = new HashMap<>();

        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenThrow(new IOException("BOOM!"));
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        try {
            this.service.get("localhost", headers, UserInfoResponse.class);
            Assertions.fail("Expectex exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error making request", ex.getMessage());
            Assertions.assertNotNull(ex.getCause());
            Assertions.assertEquals("BOOM!", ex.getCause().getMessage());
        }
    }

    @Test
    public void test_post() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-header", "junk");

        UserData requestBody = new UserData();

        Mockito.when(this.jsonService.to(requestBody)).thenReturn("appJwtRequeust-json");
        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenReturn("text");
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        String result = this.service.post("localhost", headers, String.class, requestBody);
        Assertions.assertEquals("text", result);
    }

    @Test
    public void test_post_signedRequest() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-header", "junk");

        String privateKey = this.newPrivateKey();
        SignedRequest requestBody = new SignedRequest(System.currentTimeMillis(), "app-id", "base64");

        Mockito.when(this.jsonService.to(requestBody)).thenReturn("json");
        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenReturn("text");
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        String result = this.service.post("localhost", headers, String.class, requestBody, privateKey);
        Assertions.assertEquals("text", result);
    }

    @Test
    public void test_put() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-header", "junk");

        UserData requestBody = new UserData();

        Mockito.when(this.jsonService.to(requestBody)).thenReturn("appJwtRequeust-json");
        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenReturn(null);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        this.service.put("localhost", headers, requestBody);
    }

    @Test
    public void test_put_statusError() {
        Map<String, String> headers = new HashMap<>();

        UserData requestBody = new UserData();

        Mockito.when(this.jsonService.to(requestBody)).thenReturn("json");
        try {
            Mockito.when(this.httpClient.execute( //
                    ArgumentMatchers.any(HttpUriRequestBase.class), //
                    ArgumentMatchers.any(AbstractHttpClientResponseHandler.class))). //
                    thenThrow(new HttpException("Error making request", HttpStatus.SC_OK, new ArrayList<>()));
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        try {
            this.service.put("localhost", headers, requestBody);
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals("Error making request", ex.getMessage());
            Assertions.assertEquals(HttpStatus.SC_OK, ex.getStatus());
            Assertions.assertNotNull(ex.getMessages());
        }
    }

    private String newPrivateKey() {
        KeyPair keys = this.keyGenerator.generate(Algorithms.ED448);
        return Encodings.Base64.encode(keys.getPrivate().getEncoded());
    }
}
