package com.github.lc.oss.commons.identity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.github.lc.oss.commons.api.services.JsonService;
import com.github.lc.oss.commons.identity.model.ApiResponse;
import com.github.lc.oss.commons.testing.AbstractMockTest;

public class StringResponseHandlerTest extends AbstractMockTest {
    @Mock
    private JsonService jsonService;

    private StringResponseHandler handler;

    @BeforeEach
    public void setup() {
        this.handler = new StringResponseHandler(this.jsonService);
    }

    @Test
    public void test_handleEntity_error() {
        HttpEntity entity = Mockito.mock(HttpEntity.class);

        final IOException cause = new IOException("Boom!");

        try {
            Mockito.when(entity.getContent()).thenThrow(cause);
        } catch (UnsupportedOperationException | IOException e) {
            Assertions.fail("Unexpected exception");
        }

        try {
            this.handler.handleEntity(entity);
            Assertions.fail("Expected exception");
        } catch (IOException ex) {
            Assertions.assertSame(cause, ex.getCause());
        }
    }

    @Test
    public void test_handleEntity() {
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        final String response = "text";

        InputStream stream = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));

        try {
            Mockito.when(entity.getContent()).thenReturn(stream);
        } catch (UnsupportedOperationException | IOException e) {
            Assertions.fail("Unexpected exception");
        }

        try {
            String result = this.handler.handleEntity(entity);
            Assertions.assertEquals(response, result);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_handleResponse_error_noEntity() {
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);

        Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
        Mockito.when(response.getEntity()).thenReturn(null);

        try {
            this.handler.handleResponse(response);
            Assertions.fail("Expected exception");
        } catch (IOException e) {
            Assertions.fail("Unexpected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals("Error making request", ex.getMessage());
            Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatus());
            Assertions.assertNull(ex.getMessages());
        }
    }

    @Test
    public void test_handleResponse_error_noMessages() {
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        final String responseJson = "{}";
        InputStream stream = new ByteArrayInputStream(responseJson.getBytes(StandardCharsets.UTF_8));

        ApiResponse<?> apiResponse = new ApiResponse<>();

        Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        Mockito.when(response.getEntity()).thenReturn(entity);
        try {
            Mockito.when(entity.getContent()).thenReturn(stream);
        } catch (UnsupportedOperationException | IOException e) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(this.jsonService.from(responseJson, ApiResponse.class)).thenReturn(apiResponse);

        try {
            this.handler.handleResponse(response);
            Assertions.fail("Expected exception");
        } catch (IOException e) {
            Assertions.fail("Unexpected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals("Error making request", ex.getMessage());
            Assertions.assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, ex.getStatus());
            Assertions.assertNull(ex.getMessages());
        }
    }

    @Test
    public void test_handleResponse_error_withMessages() {
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        final String responseJson = "{}";
        InputStream stream = new ByteArrayInputStream(responseJson.getBytes(StandardCharsets.UTF_8));

        ApiResponse<?> apiResponse = new ApiResponse<>();
        apiResponse.setMessages(new ArrayList<>());

        Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        Mockito.when(response.getEntity()).thenReturn(entity);
        try {
            Mockito.when(entity.getContent()).thenReturn(stream);
        } catch (UnsupportedOperationException | IOException e) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(this.jsonService.from(responseJson, ApiResponse.class)).thenReturn(apiResponse);

        try {
            this.handler.handleResponse(response);
            Assertions.fail("Expected exception");
        } catch (IOException e) {
            Assertions.fail("Unexpected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals("Error making request", ex.getMessage());
            Assertions.assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, ex.getStatus());
            Assertions.assertSame(apiResponse.getMessages(), ex.getMessages());
        }
    }

    @Test
    public void test_handleResponse_serverError() {
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        final String text = "text";
        InputStream stream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

        Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_BAD_GATEWAY);
        Mockito.when(response.getEntity()).thenReturn(entity);
        try {
            Mockito.when(entity.getContent()).thenReturn(stream);
        } catch (UnsupportedOperationException | IOException e) {
            Assertions.fail("Unexpected exception");
        }

        try {
            this.handler.handleResponse(response);
            Assertions.fail("Expected exception");
        } catch (IOException e) {
            Assertions.fail("Unexpected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals("Error making request", ex.getMessage());
            Assertions.assertEquals(HttpStatus.SC_BAD_GATEWAY, ex.getStatus());
            Assertions.assertNull(ex.getMessages());
        }
    }

    @Test
    public void test_handleResponse_error_parsingException() {
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
        HttpEntity entity = Mockito.mock(HttpEntity.class);

        Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        Mockito.when(response.getEntity()).thenReturn(entity);
        try {
            Mockito.when(entity.getContent()).thenThrow(new IOException("BOOM!"));
        } catch (UnsupportedOperationException | IOException e) {
            Assertions.fail("Unexpected exception");
        }

        try {
            this.handler.handleResponse(response);
            Assertions.fail("Expected exception");
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error getting messages", ex.getMessage());
        }
    }

    @Test
    public void test_handleResponse_ok() {
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        final String text = "text";
        InputStream stream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

        Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(response.getEntity()).thenReturn(entity);
        try {
            Mockito.when(entity.getContent()).thenReturn(stream);
        } catch (UnsupportedOperationException | IOException e) {
            Assertions.fail("Unexpected exception");
        }

        try {
            String result = this.handler.handleResponse(response);
            Assertions.assertEquals(text, result);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    /*
     * NOTE: Due to the current implementation we can't actually mock an Entity
     * response as the code never makes the call to get the Entity when a 204 status
     * is detected.
     *
     * If this changes in the future then this will become a more meaningful
     * scenario.
     */
    @Test
    public void test_handleResponse_noContent_withEntity() {
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);

        Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        try {
            String result = this.handler.handleResponse(response);
            Assertions.assertNull(result);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    /*
     * NOTE: Due to the current implementation we can't actually mock an Entity
     * response as the code never makes the call to get the Entity when a 204 status
     * is detected.
     *
     * If this changes in the future then this will become a more meaningful
     * scenario.
     */
    @Test
    public void test_handleResponse_noContent_blankEntity() {
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);

        Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        try {
            String result = this.handler.handleResponse(response);
            Assertions.assertNull(result);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_handleResponse_noContent() {
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);

        Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        try {
            String result = this.handler.handleResponse(response);
            Assertions.assertNull(result);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }
    }
}
