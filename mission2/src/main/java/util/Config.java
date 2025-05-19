package util;

public class Config {
    private static final String DEFAULT_API_URL = "https://api.care-connect.ovh";

    public static String getApiBaseUrl() {
        String env = System.getenv("CARE_CONNECT_API");
        return (env != null && !env.isEmpty()) ? env : DEFAULT_API_URL;
    }
}
