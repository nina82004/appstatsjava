package util;

public class SessionContext {
    private static String token;
    private static String userEmail;

    public static void setToken(String t) {
        token = t;
    }

    public static String getToken() {
        return token;
    }

    public static void setUserEmail(String email) {
        userEmail = email;
    }

    public static String getUserEmail() {
        return userEmail;
    }

    public static void clear() {
        token = null;
        userEmail = null;
    }
}
