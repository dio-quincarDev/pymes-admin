package auth.pymes.testutil;

import auth.pymes.common.constants.ApiPathConstants;

public final class TestApiPaths {
    private TestApiPaths() {}

    public static final String V1 = ApiPathConstants.V1_ROUTE;

    public static final String AUTH = ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE;
    public static final String AUTH_REGISTER = AUTH + ApiPathConstants.AUTH_REGISTER;
    public static final String AUTH_LOGIN = AUTH + ApiPathConstants.AUTH_LOGIN;
    public static final String AUTH_LOGOUT = AUTH + ApiPathConstants.AUTH_LOGOUT;
    public static final String AUTH_REFRESH = AUTH + ApiPathConstants.AUTH_REFRESH;
    public static final String AUTH_VERIFY_EMAIL = AUTH + ApiPathConstants.AUTH_VERIFY_EMAIL;
    public static final String AUTH_RESEND_VERIFICATION = AUTH + ApiPathConstants.AUTH_RESEND_VERIFICATION;
    public static final String AUTH_FORGOT_PASSWORD = AUTH + ApiPathConstants.AUTH_FORGOT_PASSWORD;
    public static final String AUTH_RESET_PASSWORD = AUTH + ApiPathConstants.AUTH_RESET_PASSWORD;
    public static final String AUTH_OAUTH2_INTENT = AUTH + ApiPathConstants.AUTH_OAUTH2_INTENT;
    public static final String AUTH_OAUTH2_INTENT_GET = AUTH + ApiPathConstants.AUTH_OAUTH2_INTENT_GET;

    public static final String USERS = ApiPathConstants.V1_ROUTE + ApiPathConstants.USERS_ROUTE;
    public static final String USERS_ME = USERS + ApiPathConstants.USERS_ME;

    public static final String TENANTS = ApiPathConstants.V1_ROUTE + ApiPathConstants.TENANTS_ROUTE;
    public static final String TENANTS_SELECT = TENANTS + ApiPathConstants.TENANTS_SELECT;

    public static final String MEMBERS = ApiPathConstants.V1_ROUTE + ApiPathConstants.TENANTS_ROUTE + "/{tenantId}" + ApiPathConstants.MEMBERS_ROUTE;

    public static final String INVITATIONS = ApiPathConstants.V1_ROUTE + ApiPathConstants.INVITATIONS_ROUTE;
    public static final String INVITATIONS_ACCEPT = INVITATIONS + ApiPathConstants.INVITATIONS_ACCEPT;
}