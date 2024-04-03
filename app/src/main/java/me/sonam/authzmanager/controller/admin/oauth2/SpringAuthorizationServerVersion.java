package me.sonam.authzmanager.controller.admin.oauth2;


public final class SpringAuthorizationServerVersion {
    private static final int MAJOR = 1;
    private static final int MINOR = 1;
    private static final int PATCH = 0;
    public static final long SERIAL_VERSION_UID = (long)getVersion().hashCode();

    public SpringAuthorizationServerVersion() {
    }

    public static String getVersion() {
        return "1.1.0";
    }
}
