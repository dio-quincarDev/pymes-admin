package auth.pymes.common.constants;

public class ApiPathConstants {
    // Version prefix
    public static final String V1_ROUTE = "/api/v1";

    // Base routes
    public static final String AUTH_ROUTE = "/auth";
    public static final String USERS_ROUTE = "/users";
    public static final String TENANTS_ROUTE = "/tenants";
    public static final String MEMBERS_ROUTE = "/members";
    public static final String INVITATIONS_ROUTE = "/invitations";

    // Auth sub-paths
    public static final String AUTH_REGISTER = "/register";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_LOGOUT = "/logout";
    public static final String AUTH_REFRESH = "/refresh";
    public static final String AUTH_VERIFY_EMAIL = "/verify-email";
    public static final String AUTH_RESEND_VERIFICATION = "/resend-verification";
    public static final String AUTH_FORGOT_PASSWORD = "/forgot-password";
    public static final String AUTH_RESET_PASSWORD = "/reset-password";
    
    // OAuth2 paths
    public static final String AUTH_OAUTH2 = "/oauth2";
    public static final String OAUTH2_INTENT = "/intent";
    public static final String OAUTH2_INTENT_GET = "/intent/{intentId}";

    // User sub-paths
    public static final String USERS_ME = "/me";

    // Tenant sub-paths
    public static final String TENANTS_SELECT = "/select";

    // Invitation sub-paths
    public static final String INVITATIONS_ACCEPT = "/accept";

    // Full paths for security config
    public static final String FULL_AUTH_REGISTER = V1_ROUTE + AUTH_ROUTE + AUTH_REGISTER;
    public static final String FULL_AUTH_LOGIN = V1_ROUTE + AUTH_ROUTE + AUTH_LOGIN;
    public static final String FULL_AUTH_LOGOUT = V1_ROUTE + AUTH_ROUTE + AUTH_LOGOUT;
    public static final String FULL_AUTH_REFRESH = V1_ROUTE + AUTH_ROUTE + AUTH_REFRESH;
    public static final String FULL_AUTH_VERIFY_EMAIL = V1_ROUTE + AUTH_ROUTE + AUTH_VERIFY_EMAIL;
    public static final String FULL_AUTH_RESEND_VERIFICATION = V1_ROUTE + AUTH_ROUTE + AUTH_RESEND_VERIFICATION;
    public static final String FULL_AUTH_FORGOT_PASSWORD = V1_ROUTE + AUTH_ROUTE + AUTH_FORGOT_PASSWORD;
    public static final String FULL_AUTH_RESET_PASSWORD = V1_ROUTE + AUTH_ROUTE + AUTH_RESET_PASSWORD;
    public static final String FULL_AUTH_OAUTH2_INTENT = V1_ROUTE + AUTH_ROUTE + AUTH_OAUTH2 + OAUTH2_INTENT;
}
