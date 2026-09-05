package com.aitest.api.tests;

import com.microsoft.playwright.APIResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Seed — environment baseline. Every API agent (Planner/Generator/Healer) reads this first to
 * find the base URL and confirm the target API is reachable, before doing anything else. If this
 * fails, fix it before touching any other test.
 */
public class SeedApiTest extends BaseApiTest {

  @Test(groups = "smoke", description = "JSONPlaceholder API is reachable and returns posts")
  public void apiIsReachable() {
    APIResponse response = request.get("/posts/1");

    Assert.assertEquals(response.status(), 200);
    Assert.assertTrue(response.text().contains("\"id\""));
  }
}
