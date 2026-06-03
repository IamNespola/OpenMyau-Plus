package me.ksyz.accountmanager.auth;

/*
 * This file is derived from https://github.com/ksyzov/AccountManager.
 * Originally licensed under the GNU LGPL.
 *
 * This modified version is licensed under the GNU GPL v3.
 */
public class Account {
    // Account types. Microsoft and Cookie accounts both hold real Minecraft
    // tokens and log in through the Microsoft flow; Cracked accounts hold only
    // a username and log in offline without any network request.
    public static final String TYPE_MICROSOFT = "microsoft";
    public static final String TYPE_CRACKED = "cracked";
    public static final String TYPE_COOKIE = "cookie";

    private String refreshToken;
    private String accessToken;
    private String username;
    private long unban;
    private String clientId;
    private String scope;
    private String type;

    public Account(String refreshToken, String accessToken, String username, String clientId, String scope) {
        this(refreshToken, accessToken, username, 0L, clientId, scope, TYPE_MICROSOFT);
    }

    public Account(String refreshToken, String accessToken, String username, long unban, String clientId, String scope) {
        this(refreshToken, accessToken, username, unban, clientId, scope, TYPE_MICROSOFT);
    }

    public Account(String refreshToken, String accessToken, String username, String clientId, String scope, String type) {
        this(refreshToken, accessToken, username, 0L, clientId, scope, type);
    }

    public Account(String refreshToken, String accessToken, String username, long unban, String clientId, String scope, String type) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.unban = unban;
        this.clientId = clientId;
        this.scope = scope;
        this.type = (type == null || type.isEmpty()) ? TYPE_MICROSOFT : type;
    }

    /**
     * Creates an offline (cracked) account that only carries a username.
     */
    public static Account cracked(String username) {
        return new Account("", "", username, 0L, "", "", TYPE_CRACKED);
    }

    public String getClientId() {
        return clientId;
    }

    public String getScope() {
        return scope;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getUsername() {
        return username;
    }

    public long getUnban() {
        return unban;
    }

    public String getType() {
        return type;
    }

    public boolean isCracked() {
        return TYPE_CRACKED.equals(type);
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUnban(long unban) {
        this.unban = unban;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setType(String type) {
        this.type = (type == null || type.isEmpty()) ? TYPE_MICROSOFT : type;
    }
}
