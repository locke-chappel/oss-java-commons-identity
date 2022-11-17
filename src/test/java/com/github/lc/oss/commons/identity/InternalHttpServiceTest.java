package com.github.lc.oss.commons.identity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.github.lc.oss.commons.api.identity.Messages;
import com.github.lc.oss.commons.api.services.JsonService;
import com.github.lc.oss.commons.encoding.Encodings;
import com.github.lc.oss.commons.identity.model.ApiResponse;
import com.github.lc.oss.commons.identity.model.SignedRequest;
import com.github.lc.oss.commons.identity.model.UserData;
import com.github.lc.oss.commons.identity.model.UserInfo;
import com.github.lc.oss.commons.identity.model.UserInfoResponse;
import com.github.lc.oss.commons.serialization.Message.Category;
import com.github.lc.oss.commons.serialization.Message.Severities;
import com.github.lc.oss.commons.serialization.Message.Severity;
import com.github.lc.oss.commons.signing.Algorithms;
import com.github.lc.oss.commons.signing.KeyGenerator;
import com.github.lc.oss.commons.testing.AbstractMockTest;

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

        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);

        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenReturn(response);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        this.service.delete("localhost", headers);
    }

    @Test
    public void test_delete_error_wrongStatus() {
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);

        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenReturn(response);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        try {
            this.service.delete("localhost", null);
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals("Error making request", ex.getMessage());
            Assertions.assertEquals(200, ex.getStatus());
            Assertions.assertNull(ex.getMessages());
        }
    }

    @Test
    public void test_delete_error() {
        Map<String, String> headers = new HashMap<>();

        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenThrow(new IOException("BOOM!"));
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        try {
            this.service.delete("localhost", headers);
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

        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        final String responseJson = "{}";
        StringEntity responseEntity = new StringEntity(responseJson, StandardCharsets.UTF_8);
        UserInfoResponse<UserInfo> uiResponse = new UserInfoResponse<>();

        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenReturn(response);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(response.getEntity()).thenReturn(responseEntity);
        Mockito.when(this.jsonService.from(responseJson, UserInfoResponse.class)).thenReturn(uiResponse);

        @SuppressWarnings("unchecked")
        UserInfoResponse<UserInfo> result = this.service.get("localhost", headers, UserInfoResponse.class);
        Assertions.assertNotNull(result);
    }

    @Test
    public void test_get_string() {
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        final String responseJson = "text";
        StringEntity responseEntity = new StringEntity(responseJson, StandardCharsets.UTF_8);

        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenReturn(response);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(response.getEntity()).thenReturn(responseEntity);

        String result = this.service.get("localhost", null, String.class);
        Assertions.assertEquals("text", result);
    }

    @Test
    public void test_get_string_null() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-header", "junk");

        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);

        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenReturn(response);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(response.getEntity()).thenReturn(null);

        String result = this.service.get("localhost", headers, String.class);
        Assertions.assertNull(result);
    }

    @Test
    public void test_get_closeException() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-header", "junk");

        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);

        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenReturn(response);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(response.getEntity()).thenReturn(null);
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

        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);

        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenReturn(response);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        Mockito.when(response.getEntity()).thenReturn(new StringEntity("null", StandardCharsets.UTF_8));

        try {
            this.service.get("localhost", headers, UserInfoResponse.class);
            Assertions.fail("Expectex exception");
        } catch (HttpException ex) {
            Assertions.assertEquals("Error making request", ex.getMessage());
            Assertions.assertEquals(422, ex.getStatus());
            Assertions.assertNull(ex.getMessages());
        }
    }

    @Test
    public void test_get_error_exception() {
        Map<String, String> headers = new HashMap<>();

        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenThrow(new IOException("BOOM!"));
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

        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        StringEntity responseEntity = new StringEntity("text", StandardCharsets.UTF_8);

        Mockito.when(this.jsonService.to(requestBody)).thenReturn("appJwtRequeust-json");
        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenReturn(response);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(response.getEntity()).thenReturn(responseEntity);

        String result = this.service.post("localhost", headers, String.class, requestBody);
        Assertions.assertEquals("text", result);
    }

    @Test
    public void test_post_signedRequest() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-header", "junk");

        String privateKey = this.newPrivateKey();
        SignedRequest requestBody = new SignedRequest(System.currentTimeMillis(), "app-id", "base64");

        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        StringEntity responseEntity = new StringEntity("text", StandardCharsets.UTF_8);

        Mockito.when(this.jsonService.to(requestBody)).thenReturn("json");
        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenReturn(response);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(response.getEntity()).thenReturn(responseEntity);

        String result = this.service.post("localhost", headers, String.class, requestBody, privateKey);
        Assertions.assertEquals("text", result);
    }

    @Test
    public void test_put() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-header", "junk");

        UserData requestBody = new UserData();

        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);

        Mockito.when(this.jsonService.to(requestBody)).thenReturn("appJwtRequeust-json");
        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenReturn(response);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        this.service.put("localhost", headers, requestBody);
    }

    @Test
    public void test_put_statusError() {
        Map<String, String> headers = new HashMap<>();

        UserData requestBody = new UserData();

        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        final String responseJson = "{}";
        StringEntity responseEntity = new StringEntity(responseJson, StandardCharsets.UTF_8);
        ApiResponse<?> apiResponse = new ApiResponse<>();
        Messages message = new Messages() {
            @Override
            public Category getCategory() {
                return Categories.Application;
            }

            @Override
            public Severity getSeverity() {
                return Severities.Error;
            }

            @Override
            public int getNumber() {
                return 1;
            }
        };
        apiResponse.setMessages(Arrays.asList(message));

        Mockito.when(this.jsonService.to(requestBody)).thenReturn("json");
        try {
            Mockito.when(this.httpClient.execute(ArgumentMatchers.notNull())).thenReturn(response);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(response.getEntity()).thenReturn(responseEntity);
        Mockito.when(this.jsonService.from(responseJson, ApiResponse.class)).thenReturn(apiResponse);

        try {
            this.service.put("localhost", headers, requestBody);
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals("Error making request", ex.getMessage());
            Assertions.assertEquals(200, ex.getStatus());
            Assertions.assertNotNull(ex.getMessages());
        }
    }

    private String newPrivateKey() {
        KeyPair keys = this.keyGenerator.generate(Algorithms.ED448);
        return Encodings.Base64.encode(keys.getPrivate().getEncoded());
    }
}
