package com.aitest.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads base API configuration from {@code application.properties} (and an optional
 * environment-specific override), the way {@code playwright.config.ts}'s {@code baseURL} works
 * for the UI stack. Never hardcode a base URL, header, or credential in a test or Service Object —
 * read it from here instead.
 */
public final class ApiConfig {

  private static final Properties PROPERTIES = load();

  private ApiConfig() {
  }

  private static Properties load() {
    Properties props = new Properties();
    try (InputStream defaults = ApiConfig.class.getClassLoader()
        .getResourceAsStream("application.properties")) {
      if (defaults != null) {
        props.load(defaults);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load application.properties", e);
    }

    String env = System.getProperty("test.env", System.getenv().getOrDefault("TEST_ENV", ""));
    if (!env.isBlank()) {
      String envFile = "env-" + env + ".properties";
      try (InputStream envStream = ApiConfig.class.getClassLoader().getResourceAsStream(envFile)) {
        if (envStream != null) {
          props.load(envStream);
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to load " + envFile, e);
      }
    }

    // System properties / env vars always win, e.g. -Dbase.url=... or BASE_URL=... in CI.
    props.stringPropertyNames().forEach(key -> {
      String override = System.getProperty(key);
      if (override != null) {
        props.setProperty(key, override);
      }
    });

    return props;
  }

  public static String baseUrl() {
    return required("base.url");
  }

  public static int timeoutMs() {
    return Integer.parseInt(PROPERTIES.getProperty("request.timeout.ms", "10000"));
  }

  public static String authToken() {
    // Empty for an unauthenticated demo API. For a real API, set API_AUTH_TOKEN in the
    // environment (CI secret) — never commit a real token to application.properties.
    return System.getenv().getOrDefault("API_AUTH_TOKEN", PROPERTIES.getProperty("auth.token", ""));
  }

  private static String required(String key) {
    String value = PROPERTIES.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing required config key: " + key);
    }
    return value;
  }
}
