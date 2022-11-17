package com.github.lc.oss.commons.identity.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class UserInfo implements com.github.lc.oss.commons.api.identity.UserInfo {
    private String id;
    private String databaseId;
    private String username;
    private String displayName;
    private String preferredTheme;
    private Set<String> permissions;
    private Map<String, String> userData = new HashMap<>();

    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDatabaseId() {
        return this.databaseId;
    }

    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getPreferredTheme() {
        return this.preferredTheme;
    }

    public void setPreferredTheme(String preferredTheme) {
        this.preferredTheme = preferredTheme;
    }

    @Override
    public Set<String> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public Map<String, String> getUserData() {
        return this.userData;
    }

    public void setUserData(Map<String, String> userData) {
        this.userData = userData;
    }
}
