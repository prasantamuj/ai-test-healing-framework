package com.aitest.api.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Seed — environment baseline. Every API agent (Planner/Generator/Healer) reads this first to
 * find the base URL and confirm the target API is reachable, before doing anything else. If this
 * fails, fix it before touching any other test.
 */
@Tag("smoke")
class SeedApiTest extends BaseApiTest {

  @Test
  @DisplayName("JSONPlaceholder API is reachable and returns posts")
  void apiIsReachable() {
    APIResponse response = request.get("/posts/1");

    assertEquals(200, response.status());
    assertTrue(response.text().contains("\"id\""));
  }
}
