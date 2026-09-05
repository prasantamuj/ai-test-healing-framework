package com.aitest.api.tests;

import com.aitest.api.config.ApiConfig;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import io.qameta.allure.testng.AllureTestNg;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;

/**
 * Base class every API test class extends. Owns the {@link Playwright} instance and the shared
 * {@link APIRequestContext} (base URL + default headers + auth), the API equivalent of what
 * {@code BasePage} + the Playwright {@code page} fixture provide on the UI side.
 *
 * <p>Never construct an {@link APIRequestContext} directly in a test — always get it from here.
 */
@Listeners(AllureTestNg.class)
public abstract class BaseApiTest {

  private static Playwright playwright;
  protected static APIRequestContext request;

  @BeforeClass
  static void setUpRequestContext() {
    playwright = Playwright.create();

    Map<String, String> defaultHeaders = new HashMap<>();
    defaultHeaders.put("Content-Type", "application/json");
    defaultHeaders.put("Accept", "application/json");
    if (!ApiConfig.authToken().isBlank()) {
      defaultHeaders.put("Authorization", "Bearer " + ApiConfig.authToken());
    }

    request = playwright.request().newContext(new APIRequest.NewContextOptions()
        .setBaseURL(ApiConfig.baseUrl())
        .setExtraHTTPHeaders(defaultHeaders)
        .setTimeout(ApiConfig.timeoutMs()));
  }

  @AfterClass
  static void tearDownRequestContext() {
    if (request != null) {
      request.dispose();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  /** Escape hatch for a scenario that legitimately needs a custom request option (e.g. a
   *  one-off header). Prefer a Service Object method over calling this from a test body. */
  protected static RequestOptions options() {
    return RequestOptions.create();
  }
}
