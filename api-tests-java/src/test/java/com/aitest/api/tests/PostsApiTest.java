package com.aitest.api.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aitest.api.clients.PostsApiClient;
import com.aitest.api.model.ApiResponse;
import com.aitest.api.model.Post;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Covers specs/jsonplaceholder-posts.md, scenarios 1.1-1.4.
 * Seed: src/test/java/com/aitest/api/tests/SeedApiTest.java
 */
class PostsApiTest extends BaseApiTest {

  private PostsApiClient posts;

  @BeforeEach
  void setUpClient() {
    posts = new PostsApiClient(request);
  }

  @Test
  @Tag("smoke")
  @Tag("critical")
  @DisplayName("Scenario 1.1 - GET /posts/1 returns the expected post")
  void getSinglePostById() {
    ApiResponse<Post> response = posts.getPostById(1);

    assertEquals(200, response.status());
    assertNotNull(response.body(), "response body should parse into a Post");
    assertEquals(1, response.body().getId());
    assertTrue(response.body().getUserId() > 0);
    assertFalse(response.body().getTitle().isBlank());
  }

  @Test
  @Tag("regression")
  @DisplayName("Scenario 1.2 - GET /posts/{invalid-id} returns 404")
  void getPostByInvalidIdReturnsNotFound() {
    ApiResponse<Post> response = posts.getPostById(999_999);

    assertEquals(404, response.status());
  }

  @Test
  @Tag("regression")
  @DisplayName("Scenario 1.3 - GET /posts?userId=1 returns only that user's posts")
  void getPostsFilteredByUserId() {
    ApiResponse<List<Post>> response = posts.getPostsByUserId(1);

    assertEquals(200, response.status());
    assertNotNull(response.body());
    assertFalse(response.body().isEmpty());
    assertTrue(response.body().stream().allMatch(post -> post.getUserId() == 1));
  }

  @Test
  @Tag("regression")
  @DisplayName("Scenario 1.4 - POST /posts creates a post and echoes the payload")
  void createPostEchoesPayload() {
    Post newPost = new Post(1, "Healing loop demo post", "Created by the API test suite");

    ApiResponse<Post> response = posts.createPost(newPost);

    assertEquals(201, response.status());
    assertNotNull(response.body());
    assertEquals(newPost.getTitle(), response.body().getTitle());
    assertEquals(newPost.getBody(), response.body().getBody());
    assertTrue(response.body().getId() > 0, "API should assign a new id");
  }
}
