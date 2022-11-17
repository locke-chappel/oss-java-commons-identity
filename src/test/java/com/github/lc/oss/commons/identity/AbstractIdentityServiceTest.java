package com.github.lc.oss.commons.identity;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.github.lc.oss.commons.api.services.JsonService;
import com.github.lc.oss.commons.identity.model.ApplicationInfo;
import com.github.lc.oss.commons.identity.model.ApplicationInfoResponse;
import com.github.lc.oss.commons.identity.model.Token;
import com.github.lc.oss.commons.identity.model.TokenResponse;
import com.github.lc.oss.commons.identity.model.UserData;
import com.github.lc.oss.commons.identity.model.UserInfo;
import com.github.lc.oss.commons.identity.model.UserInfoBatchResponse;
import com.github.lc.oss.commons.identity.model.UserInfoResponse;
import com.github.lc.oss.commons.identity.model.UserInfoSet;
import com.github.lc.oss.commons.testing.AbstractMockTest;

public class AbstractIdentityServiceTest extends AbstractMockTest {
    private static class TestService extends AbstractIdentityService {
        @Override
        protected String getApplicationId() {
            return null;
        }

        @Override
        protected String getApplicationPrivateKey() {
            return null;
        }

        @Override
        protected String getIdentityId() {
            return null;
        }

        @Override
        protected String getIdentityUrl() {
            return null;
        }
    }

    private abstract static class ThreadHelper implements Runnable {
        private BlockingQueue<Object> lock = new ArrayBlockingQueue<>(1);
        protected AbstractIdentityService identityService;

        public ThreadHelper(AbstractIdentityService service) {
            this.identityService = service;
        }

        protected void lock() {
            try {
                this.lock.take();
            } catch (InterruptedException e) {
                Assertions.fail("Unexpectex exception");
            }
        }

        public void unlock() {
            this.lock.offer(new Object());
        }

        @Override
        public void run() {
            this.lock();

            this.process();
        }

        protected abstract void process();
    }

    private static class CallHelper {
        public int count = 0;
    }

    @Mock
    private JsonService jsonService;
    @Mock
    private HttpService httpService;

    private AbstractIdentityService service;

    @BeforeEach
    public void setup() {
        this.service = new TestService();
        this.setField("jsonService", this.jsonService, this.service);
        this.setField("httpService", this.httpService, this.service);

        this.setField("applicationToken", new Token(System.currentTimeMillis() + 100000, "token-value"), this.service);
    }

    @Test
    public void test_init() {
        AbstractIdentityService service = new TestService();
        Assertions.assertNull(this.getField("httpService", service));

        service.init(this.jsonService);

        Assertions.assertNotNull(this.getField("httpService", service));
    }

    @Test
    public void test_clearApplicationToken() {
        AbstractIdentityService service = new TestService();

        this.setField("applicationToken", new Token(-1l, "token-value"), service);
        Assertions.assertNotNull(this.getField("applicationToken", service));

        service.clearApplicationToken();
        Assertions.assertNull(this.getField("applicationToken", service));
    }

    @Test
    public void test_getApplicationInfo() {
        ApplicationInfoResponse response = new ApplicationInfoResponse();
        ApplicationInfo appInfo = new ApplicationInfo();
        response.setBody(appInfo);

        Mockito.when(this.httpService.get(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull())).thenReturn(response);

        ApplicationInfo result = this.service.getApplicationInfo();
        Assertions.assertSame(appInfo, result);
    }

    @Test
    public void test_getApplicationInfo_notFound() {
        Mockito.when(this.httpService.get(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull()))
                .thenThrow(new HttpException("NotFound", 404, null));

        try {
            this.service.getApplicationInfo();
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(404, ex.getStatus());
        }
    }

    @Test
    public void test_getApplicationInfo_retry() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        final CallHelper helper = new CallHelper();

        ApplicationInfoResponse response = new ApplicationInfoResponse();
        ApplicationInfo appInfo = new ApplicationInfo();
        response.setBody(appInfo);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<ApplicationInfoResponse>() {
            @Override
            public ApplicationInfoResponse answer(InvocationOnMock invocation) throws Throwable {
                if (helper.count < 1) {
                    helper.count++;
                    throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
                }

                return response;
            }
        }).when(this.httpService).get(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        Assertions.assertEquals(0, helper.count);

        service.getApplicationInfo();

        Assertions.assertEquals(1, helper.count);
    }

    @Test
    public void test_getApplicationInfo_retryFail() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        ApplicationInfoResponse response = new ApplicationInfoResponse();
        ApplicationInfo appInfo = new ApplicationInfo();
        response.setBody(appInfo);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<ApplicationInfoResponse>() {
            @Override
            public ApplicationInfoResponse answer(InvocationOnMock invocation) throws Throwable {
                throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
            }
        }).when(this.httpService).get(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        try {
            service.getApplicationInfo();
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(401, ex.getStatus());
        }
    }

    @Test
    public void test_getApplicationToken() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);

        final String result1 = service.getApplicationToken();
        Assertions.assertEquals(tokenResponse.getBody().getToken(), result1);

        final String result2 = service.getApplicationToken();
        Assertions.assertSame(result1, result2);
    }

    @Test
    public void test_getApplicationToken_expired() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        Token token1 = new Token(System.currentTimeMillis() - 100000, "token-value1");

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value2"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);

        this.setField("applicationToken", token1, service);

        final String result1 = service.getApplicationToken();
        Assertions.assertEquals(tokenResponse.getBody().getToken(), result1);
        Assertions.assertNotEquals(token1.getToken(), result1);

        final String result2 = service.getApplicationToken();
        Assertions.assertSame(result1, result2);
    }

    @Test
    public void test_getApplicationToken_aboutToExpire() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        Token token1 = new Token(System.currentTimeMillis() + 5000, "token-value1");

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value2"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);

        this.setField("applicationToken", token1, service);

        final String result1 = service.getApplicationToken();
        Assertions.assertEquals(tokenResponse.getBody().getToken(), result1);
        Assertions.assertNotEquals(token1.getToken(), result1);

        final String result2 = service.getApplicationToken();
        Assertions.assertSame(result1, result2);
    }

    @Test
    public void test_getAppToken_threading() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        final String tokenValue = "token";
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 5000, tokenValue));
        final BlockingQueue<Object> threadLock = new ArrayBlockingQueue<>(2);

        ThreadHelper getter1 = new ThreadHelper(service) {
            @Override
            protected void process() {
                Assertions.assertSame(tokenValue, this.identityService.getApplicationToken());
                threadLock.offer(new Object());
            }
        };

        ThreadHelper getter2 = new ThreadHelper(service) {
            @Override
            protected void process() {
                Assertions.assertSame(tokenValue, this.identityService.getApplicationToken());
                threadLock.offer(new Object());
            }
        };

        Thread getterT1 = new Thread(getter1);
        getterT1.setDaemon(true);
        getterT1.setName("Junit-AbstractIdentityService-Getter-1");
        getterT1.start();

        Thread getterT2 = new Thread(getter2);
        getterT2.setDaemon(true);
        getterT2.setName("Junit-AbstractIdentityService-Getter-2");
        getterT2.start();

        final CallHelper helper = new CallHelper();

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.doAnswer(new Answer<TokenResponse>() {
            @Override
            public TokenResponse answer(InvocationOnMock invocation) throws Throwable {
                helper.count++;
                waitFor(500 * helper.count);
                return tokenResponse;
            }
        }).when(this.httpService).post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

        Assertions.assertEquals(0, helper.count);

        this.setField("applicationToken", null, service);

        // --- begin!
        getter1.unlock();
        getter2.unlock();

        /*
         * dynamic waiting logic to ensure threads finish before the test attempts to
         * assert the result
         */
        this.waitUntil(() -> threadLock.poll() != null, 5000);
        this.waitUntil(() -> threadLock.poll() != null, 5000);

        this.waitUntil(() -> helper.count != 0);

        Assertions.assertEquals(2, helper.count);
        Assertions.assertSame(tokenValue, service.getApplicationToken());
    }

    @Test
    public void test_getAppToken_threading_v2() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        final String tokenValue = "token";
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, tokenValue));
        final BlockingQueue<Object> threadLock = new ArrayBlockingQueue<>(2);

        ThreadHelper getter1 = new ThreadHelper(service) {
            @Override
            protected void process() {
                Assertions.assertSame(tokenValue, this.identityService.getApplicationToken());
                threadLock.offer(new Object());
            }
        };

        ThreadHelper getter2 = new ThreadHelper(service) {
            @Override
            protected void process() {
                Assertions.assertSame(tokenValue, this.identityService.getApplicationToken());
                threadLock.offer(new Object());
            }
        };

        Thread getterT1 = new Thread(getter1);
        getterT1.setDaemon(true);
        getterT1.setName("Junit-AbstractIdentityService-Getter-1");
        getterT1.start();

        Thread getterT2 = new Thread(getter2);
        getterT2.setDaemon(true);
        getterT2.setName("Junit-AbstractIdentityService-Getter-2");
        getterT2.start();

        final CallHelper helper = new CallHelper();

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.doAnswer(new Answer<TokenResponse>() {
            @Override
            public TokenResponse answer(InvocationOnMock invocation) throws Throwable {
                helper.count++;
                return tokenResponse;
            }
        }).when(this.httpService).post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

        Assertions.assertEquals(0, helper.count);

        this.setField("applicationToken", null, service);

        // --- begin!
        getter1.unlock();
        getter2.unlock();

        /*
         * dynamic waiting logic to ensure threads finish before the test attempts to
         * assert the result
         */
        this.waitUntil(() -> threadLock.poll() != null);
        this.waitUntil(() -> threadLock.poll() != null);

        this.waitUntil(() -> helper.count != 0);

        Assertions.assertEquals(1, helper.count);
        Assertions.assertSame(tokenValue, service.getApplicationToken());
    }

    @Test
    public void test_getUserInfo() {
        UserInfoResponse<UserInfo> response = new UserInfoResponse<>();
        UserInfo userInfo = new UserInfo();
        response.setBody(userInfo);

        Mockito.when(this.httpService.get(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull())).thenReturn(response);

        UserInfo result = this.service.getUserInfo("euid");
        Assertions.assertSame(userInfo, result);
    }

    @Test
    public void test_getUserInfoNotFound() {
        UserInfoResponse<UserInfo> response = new UserInfoResponse<>();

        Mockito.when(this.httpService.get(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull())).thenReturn(response);

        UserInfo result = this.service.getUserInfo("euid");
        Assertions.assertNull(result);
    }

    @Test
    public void test_getUserInfoNotFound_v2() {
        Mockito.when(this.httpService.get(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull())).thenReturn(null);

        UserInfo result = this.service.getUserInfo("euid");
        Assertions.assertNull(result);
    }

    @Test
    public void test_getUserInfoNotFound_v3() {
        HttpException error = new HttpException("Not Found", 404, null);

        Mockito.when(this.httpService.get(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull())).thenThrow(error);

        try {
            this.service.getUserInfo("euid");
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(ex.getStatus(), 404);
        }
    }

    @Test
    public void test_getUserInfoNotFound_retry() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        final CallHelper helper = new CallHelper();

        UserInfoResponse<UserInfo> response = new UserInfoResponse<>();
        UserInfo userInfo = new UserInfo();
        response.setBody(userInfo);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<UserInfoResponse<UserInfo>>() {
            @Override
            public UserInfoResponse<UserInfo> answer(InvocationOnMock invocation) throws Throwable {
                if (helper.count < 1) {
                    helper.count++;
                    throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
                }

                return response;
            }
        }).when(this.httpService).get(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        Assertions.assertEquals(0, helper.count);

        service.getUserInfo("euid");

        Assertions.assertEquals(1, helper.count);
    }

    @Test
    public void test_getUserInfoNotFound_retryFail() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        UserInfoResponse<UserInfo> response = new UserInfoResponse<>();
        UserInfo userInfo = new UserInfo();
        response.setBody(userInfo);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<UserInfoResponse<UserInfo>>() {
            @Override
            public UserInfoResponse<UserInfo> answer(InvocationOnMock invocation) throws Throwable {
                throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
            }
        }).when(this.httpService).get(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        try {
            service.getUserInfo("euid");
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(401, ex.getStatus());
        }
    }

    @Test
    public void test_getUserInfos() {
        UserInfoBatchResponse response = new UserInfoBatchResponse();
        UserInfoSet userInfoSet = new UserInfoSet();
        response.setBody(userInfoSet);

        Mockito.when(this.httpService.post(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull()))
                .thenReturn(response);

        UserInfoSet result = this.service.getUserInfos("euid");
        Assertions.assertSame(userInfoSet, result);
    }

    @Test
    public void test_getUserInfos_notFound() {
        Mockito.when(this.httpService.post(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull()))
                .thenThrow(new HttpException("NotFound", 404, null));

        try {
            this.service.getUserInfos("euid");
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(404, ex.getStatus());
        }
    }

    @Test
    public void test_getUserInfos_retry() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        final CallHelper helper = new CallHelper();

        UserInfoBatchResponse response = new UserInfoBatchResponse();
        UserInfoSet userInfoSet = new UserInfoSet();
        response.setBody(userInfoSet);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<UserInfoBatchResponse>() {
            @Override
            public UserInfoBatchResponse answer(InvocationOnMock invocation) throws Throwable {
                if (helper.count < 1) {
                    helper.count++;
                    throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
                }

                return response;
            }
        }).when(this.httpService).post(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        Assertions.assertEquals(0, helper.count);

        service.getUserInfos("euid");

        Assertions.assertEquals(1, helper.count);
    }

    @Test
    public void test_getUserInfos_retryFail() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        UserInfoBatchResponse response = new UserInfoBatchResponse();
        UserInfoSet userInfoSet = new UserInfoSet();
        response.setBody(userInfoSet);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<UserInfoBatchResponse>() {
            @Override
            public UserInfoBatchResponse answer(InvocationOnMock invocation) throws Throwable {
                throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
            }
        }).when(this.httpService).post(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        try {
            service.getUserInfos("euid");
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(401, ex.getStatus());
        }
    }

    @Test
    public void test_deleteUserData() {
        this.service.deleteUserData("euid", "key");
    }

    @Test
    public void test_deleteUserData_notFound() {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                throw new HttpException("NotFound", 404, null);
            }
        }).when(this.httpService).delete(ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        try {
            this.service.deleteUserData("euid", "key");
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(404, ex.getStatus());
        }
    }

    @Test
    public void test_deleteUserData_retry() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        final CallHelper helper = new CallHelper();

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                if (helper.count < 1) {
                    helper.count++;
                    throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
                }
                return null;
            }
        }).when(this.httpService).delete(ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        Assertions.assertEquals(0, helper.count);

        service.deleteUserData("euid", "key");

        Assertions.assertEquals(1, helper.count);
    }

    @Test
    public void test_deleteUserData_retryFail() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
            }
        }).when(this.httpService).delete(ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        try {
            service.deleteUserData("euid", "key");
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(401, ex.getStatus());
        }
    }

    @Test
    public void test_putUserData() {
        this.service.putUserData("euid", new UserData());
    }

    @Test
    public void test_putUserData_notFound() {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                throw new HttpException("NotFound", 404, null);
            }
        }).when(this.httpService).put(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        try {
            this.service.putUserData("euid", new UserData());
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(404, ex.getStatus());
        }
    }

    @Test
    public void test_putUserData_retry() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        final CallHelper helper = new CallHelper();

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                if (helper.count < 1) {
                    helper.count++;
                    throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
                }
                return null;
            }
        }).when(this.httpService).put(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        Assertions.assertEquals(0, helper.count);

        service.putUserData("euid", new UserData());

        Assertions.assertEquals(1, helper.count);
    }

    @Test
    public void test_putUserData_retryFail() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
            }
        }).when(this.httpService).put(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        try {
            service.putUserData("euid", new UserData());
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(401, ex.getStatus());
        }
    }

    @Test
    public void test_refreshSession() {
        TokenResponse response = new TokenResponse();
        Token token = new Token(-1l, "value");
        response.setBody(token);

        Mockito.when(this.httpService.post(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull()))
                .thenReturn(response);

        String result = this.service.refreshSession("token");
        Assertions.assertSame(token.getToken(), result);
    }

    @Test
    public void test_refreshSession_null() {
        TokenResponse response = new TokenResponse();
        response.setBody(null);

        Mockito.when(this.httpService.post(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull()))
                .thenReturn(response);

        String result = this.service.refreshSession("token");
        Assertions.assertNull(result);
    }

    @Test
    public void test_refreshSession_null_v2() {
        Mockito.when(this.httpService.post(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull()))
                .thenReturn(null);

        String result = this.service.refreshSession("token");
        Assertions.assertNull(result);
    }

    @Test
    public void test_refreshSession_notFound() {
        Mockito.when(this.httpService.post(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull()))
                .thenThrow(new HttpException("NotFound", 404, null));

        try {
            this.service.refreshSession("token");
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(404, ex.getStatus());
        }
    }

    @Test
    public void test_refreshSession_retry() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        final CallHelper helper = new CallHelper();

        TokenResponse response = new TokenResponse();
        Token token = new Token(-1l, "value");
        response.setBody(token);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<TokenResponse>() {
            @Override
            public TokenResponse answer(InvocationOnMock invocation) throws Throwable {
                if (helper.count < 1) {
                    helper.count++;
                    throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
                }

                return response;
            }
        }).when(this.httpService).post(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        Assertions.assertEquals(0, helper.count);

        service.refreshSession("token");

        Assertions.assertEquals(1, helper.count);
    }

    @Test
    public void test_refreshSession_retryFail() {
        AbstractIdentityService service = new TestService();
        this.setField("jsonService", this.jsonService, service);
        this.setField("httpService", this.httpService, service);

        TokenResponse response = new TokenResponse();
        Token token = new Token(-1l, "value");
        response.setBody(token);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setBody(new Token(System.currentTimeMillis() + 100000, "token-value"));

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("signed-json");
        Mockito.when(
                this.httpService.post(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(tokenResponse);
        Mockito.doAnswer(new Answer<TokenResponse>() {
            @Override
            public TokenResponse answer(InvocationOnMock invocation) throws Throwable {
                throw new HttpException("Unauthorized", HttpStatus.SC_UNAUTHORIZED, null);
            }
        }).when(this.httpService).post(ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());

        try {
            service.refreshSession("token");
            Assertions.fail("Expected exception");
        } catch (HttpException ex) {
            Assertions.assertEquals(401, ex.getStatus());
        }
    }
}
