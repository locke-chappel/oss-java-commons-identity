package io.github.lc.oss.commons.identity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;

import io.github.lc.oss.commons.api.identity.ApiObject;
import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.identity.model.ApiResponse;
import io.github.lc.oss.commons.identity.model.AppJwtRequest;
import io.github.lc.oss.commons.identity.model.ApplicationInfo;
import io.github.lc.oss.commons.identity.model.ApplicationInfoResponse;
import io.github.lc.oss.commons.identity.model.ReissueTokenRequest;
import io.github.lc.oss.commons.identity.model.SignedRequest;
import io.github.lc.oss.commons.identity.model.Token;
import io.github.lc.oss.commons.identity.model.TokenResponse;
import io.github.lc.oss.commons.identity.model.UserData;
import io.github.lc.oss.commons.identity.model.UserInfo;
import io.github.lc.oss.commons.identity.model.UserInfoBatchResponse;
import io.github.lc.oss.commons.identity.model.UserInfoResponse;
import io.github.lc.oss.commons.identity.model.UserInfoSet;

public abstract class AbstractIdentityService {
    private static final long DEFAULT_APP_TOKEN_TTL = 24 * 60 * 60 * 1000;
    private static final long APP_TOKEN_EXPIRATION_BUFFER = 30 * 1000;
    private static final List<String> DEFAULT_APP_TOKEN_PERMISSIONS = Collections.unmodifiableList(Arrays.asList("API"));

    private HttpService httpService;
    private JsonService jsonService;

    private Token applicationToken;
    private final Object applicationTokenLock = new Object();

    public void init(JsonService jsonService) {
        this.init(jsonService, new InternalHttpService(jsonService));
    }

    public void init(JsonService jsonService, HttpService httpService) {
        this.jsonService = jsonService;
        this.httpService = httpService;
    }

    protected abstract String getApplicationId();

    protected abstract String getApplicationPrivateKey();

    protected abstract String getIdentityId();

    protected abstract String getIdentityUrl();

    protected long getApplicationTokenTtl() {
        return AbstractIdentityService.DEFAULT_APP_TOKEN_TTL;
    }

    protected List<String> getApplicationTokenPermissions() {
        return AbstractIdentityService.DEFAULT_APP_TOKEN_PERMISSIONS;
    }

    public void clearApplicationToken() {
        synchronized (this.applicationTokenLock) {
            this.applicationToken = null;
        }
    }

    public ApplicationInfo getApplicationInfo() {
        return this.getApplicationInfo(1);
    }

    private ApplicationInfo getApplicationInfo(int retryCount) {
        String url = this.getIdentityUrl() + "/svc/v1/applicationInfo";
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + this.getApplicationToken());
        try {
            ApplicationInfoResponse response = this.httpService.get(url, headers, ApplicationInfoResponse.class);
            return this.getResponseValue(response);
        } catch (HttpException ex) {
            if (ex.getStatus() == HttpStatus.SC_UNAUTHORIZED && retryCount > 0) {
                this.clearApplicationToken();
                return this.getApplicationInfo(retryCount - 1);
            } else {
                throw ex;
            }
        }
    }

    public String getApplicationToken() {
        if (this.isAppTokenInvalid()) {
            synchronized (this.applicationTokenLock) {
                if (this.isAppTokenInvalid()) {
                    String url = this.getIdentityUrl() + "/svc/v1/jwt/application";

                    Map<String, Collection<String>> permissions = new HashMap<>();
                    permissions.put(this.getIdentityId(), this.getApplicationTokenPermissions());
                    AppJwtRequest body = new AppJwtRequest(this.getApplicationTokenTtl(), permissions);

                    SignedRequest signed = new SignedRequest(System.currentTimeMillis(), this.getApplicationId(),
                            Encodings.Base64.encode(this.jsonService.to(body)));
                    TokenResponse response = this.httpService.post(url, new HashMap<>(), TokenResponse.class, signed, this.getApplicationPrivateKey());
                    this.applicationToken = response.getBody();
                }
            }
        }
        return this.applicationToken.getToken();
    }

    @SuppressWarnings("unchecked")
    public UserInfo getUserInfo(String userExternalId) {
        return this.getUserInfo(userExternalId, UserInfoResponse.class);
    }

    public <T extends UserInfo, R extends UserInfoResponse<T>> T getUserInfo(String userExternalId, Class<R> responseClass) {
        return this.getUserInfo(userExternalId, 1, responseClass);
    }

    private <T extends UserInfo, R extends UserInfoResponse<T>> T getUserInfo(String userExternalId, int retryCount, Class<R> responseClass) {
        String url = this.getIdentityUrl() + "/svc/v1/userInfo/" + userExternalId;
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + this.getApplicationToken());
        try {
            UserInfoResponse<T> response = this.httpService.get(url, headers, responseClass);
            return this.getResponseValue(response);
        } catch (HttpException ex) {
            if (ex.getStatus() == HttpStatus.SC_UNAUTHORIZED && retryCount > 0) {
                this.clearApplicationToken();
                return this.getUserInfo(userExternalId, retryCount - 1, responseClass);
            } else {
                throw ex;
            }
        }
    }

    public UserInfoSet getUserInfos(String... userExternalIds) {
        return this.getUserInfos(Arrays.asList(userExternalIds));
    }

    public UserInfoSet getUserInfos(Collection<String> userExternalIds) {
        return this.getUserInfos(userExternalIds, 1);
    }

    private UserInfoSet getUserInfos(Collection<String> userExternalIds, int retryCount) {
        String url = this.getIdentityUrl() + "/svc/v1/userInfo";
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + this.getApplicationToken());
        JsonableStringSet ids = new JsonableStringSet(userExternalIds);
        try {
            UserInfoBatchResponse response = this.httpService.post(url, headers, UserInfoBatchResponse.class, ids);
            return this.getResponseValue(response);
        } catch (HttpException ex) {
            if (ex.getStatus() == HttpStatus.SC_UNAUTHORIZED && retryCount > 0) {
                this.clearApplicationToken();
                return this.getUserInfos(userExternalIds, retryCount - 1);
            } else {
                throw ex;
            }
        }
    }

    public void deleteUserData(String userExternalId, String key) {
        this.deleteUserData(userExternalId, key, 1);
    }

    private void deleteUserData(String userExternalId, String key, int retryCount) {
        String url = this.getIdentityUrl() + "/svc/v1/userData/" + userExternalId + "/" + key;
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + this.getApplicationToken());
        try {
            this.httpService.delete(url, headers);
        } catch (HttpException ex) {
            if (ex.getStatus() == HttpStatus.SC_UNAUTHORIZED && retryCount > 0) {
                this.clearApplicationToken();
                this.deleteUserData(userExternalId, key, retryCount - 1);
            } else {
                throw ex;
            }
        }
    }

    public void putUserData(String userExternalId, UserData userData) {
        this.putUserData(userExternalId, userData, 1);
    }

    private void putUserData(String userExternalId, UserData userData, int retryCount) {
        String url = this.getIdentityUrl() + "/svc/v1/userData/" + userExternalId;
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + this.getApplicationToken());
        try {
            this.httpService.put(url, headers, userData);
        } catch (HttpException ex) {
            if (ex.getStatus() == HttpStatus.SC_UNAUTHORIZED && retryCount > 0) {
                this.clearApplicationToken();
                this.putUserData(userExternalId, userData, retryCount - 1);
            } else {
                throw ex;
            }
        }
    }

    public String refreshSession(String token) {
        return this.refreshSession(token, 1);
    }

    private String refreshSession(String token, int retryCount) {
        ReissueTokenRequest request = new ReissueTokenRequest(token, this.getApplicationId());

        String url = this.getIdentityUrl() + "/svc/v1/jwt/reissue";
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + this.getApplicationToken());

        try {
            TokenResponse response = this.httpService.post(url, headers, TokenResponse.class, request);
            if (response == null || response.getBody() == null) {
                return null;
            }
            return response.getBody().getToken();
        } catch (HttpException ex) {
            if (ex.getStatus() == HttpStatus.SC_UNAUTHORIZED && retryCount > 0) {
                this.clearApplicationToken();
                return this.refreshSession(token, retryCount - 1);
            } else {
                throw ex;
            }
        }
    }

    /*
     * Exposed for testing only
     */
    boolean isAppTokenInvalid() {
        if (this.applicationToken == null) {
            return true;
        }

        if (System.currentTimeMillis() >= this.applicationToken.getExpiration() - AbstractIdentityService.APP_TOKEN_EXPIRATION_BUFFER) {
            return true;
        }

        return false;
    }

    private <T extends ApiObject> T getResponseValue(ApiResponse<T> response) {
        if (response == null) {
            return null;
        }

        return response.getBody();
    }
}
