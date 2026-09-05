package com.aitest.api.tests;

import com.aitest.api.clients.PostsApiClient;
import com.aitest.api.model.ApiResponse;
import com.aitest.api.model.Post;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Covers specs/jsonplaceholder-posts.md, scenarios 1.1-1.4.
 * Seed: src/test/java/com/aitest/api/tests/SeedApiTest.java
 */
public class PostsApiTest extends BaseApiTest {

  private PostsApiClient posts;

  @BeforeMethod
  public void setUpClient() {
    posts = new PostsApiClient(request);
  }

  @Test(groups = {"smoke", "critical"},
      description = "Scenario 1.1 - GET /posts/1 returns the expected post")
  public void getSinglePostById() {
    ApiResponse<Post> response = posts.getPostById(1);

    Assert.assertEquals(response.status(), 200);
    Assert.assertNotNull(response.body(), "response body should parse into a Post");
    Assert.assertEquals(response.body().getId(), 1);
    Assert.assertTrue(response.body().getUserId() > 0);
    Assert.assertFalse(response.body().getTitle().isBlank());
  }

  @Test(groups = "regression",
      description = "Scenario 1.2 - GET /posts/{invalid-id} returns 404")
  public void getPostByInvalidIdReturnsNotFound() {
    ApiResponse<Post> response = posts.getPostById(999_999);

    Assert.assertEquals(response.status(), 404);
  }

  @Test(groups = "regression",
      description = "Scenario 1.3 - GET /posts?userId=1 returns only that user's posts")
  public void getPostsFilteredByUserId() {
    ApiResponse<List<Post>> response = posts.getPostsByUserId(1);

    Assert.assertEquals(response.status(), 200);
    Assert.assertNotNull(response.body());
    Assert.assertFalse(response.body().isEmpty());
    Assert.assertTrue(response.body().stream().allMatch(post -> post.getUserId() == 1));
  }

  @Test(groups = "regression",
      description = "Scenario 1.4 - POST /posts creates a post and echoes the payload")
  public void createPostEchoesPayload() {
    Post newPost = new Post(1, "Healing loop demo post", "Created by the API test suite");

    ApiResponse<Post> response = posts.createPost(newPost);

    Assert.assertEquals(response.status(), 201);
    Assert.assertNotNull(response.body());
    Assert.assertEquals(response.body().getTitle(), newPost.getTitle());
    Assert.assertEquals(response.body().getBody(), newPost.getBody());
    Assert.assertTrue(response.body().getId() > 0, "API should assign a new id");
  }
}
