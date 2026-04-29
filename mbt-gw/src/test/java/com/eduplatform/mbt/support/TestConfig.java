package com.eduplatform.mbt.support;

public final class TestConfig {

    private TestConfig() {}

    public static String webBaseUrl() {
        return sys("webBaseUrl", "WEB_BASE_URL", "http://localhost:3000");
    }

    public static String apiBaseUrl() {
        return sys("apiBaseUrl", "API_BASE_URL", "http://localhost:8000");
    }

    public static String browser() {
        return sys("selenide.browser", "SELENIDE_BROWSER", "chrome");
    }

    public static boolean headless() {
        return Boolean.parseBoolean(sys("selenide.headless", "SELENIDE_HEADLESS", "true"));
    }

    public static long timeoutMs() {
        return Long.parseLong(sys("selenide.timeout", "SELENIDE_TIMEOUT", "8000"));
    }

    private static String sys(String prop, String env, String dflt) {
        String p = System.getProperty(prop);
        if (p != null && !p.isBlank()) return p;
        String e = System.getenv(env);
        if (e != null && !e.isBlank()) return e;
        return dflt;
    }
}
