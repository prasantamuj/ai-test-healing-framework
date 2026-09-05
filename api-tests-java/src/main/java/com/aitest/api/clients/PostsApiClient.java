package com.aitest.api.clients;

import com.aitest.api.model.ApiResponse;
import com.aitest.api.model.Post;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service Object for the {@code /posts} resource — the API equivalent of a Page Object.
 *
 * <p>Contract (mirrors the UI stack's Page Object contract): constructor takes only the shared
 * {@link APIRequestContext}, every method returns a typed response, and this class never asserts
 * anything — assertions belong in the test classes only.
 */
public final class PostsApiClient {

  private static final Logger log = LoggerFactory.getLogger(PostsApiClient.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final APIRequestContext request;

  public PostsApiClient(APIRequestContext request) {
    this.request = request;
  }

  public ApiResponse<Post> getPostById(int id) {
    APIResponse response = request.get("/posts/" + id);
    return toResponse(response, Post.class);
  }

  public ApiResponse<List<Post>> getAllPosts() {
    APIResponse response = request.get("/posts");
    return toListResponse(response);
  }

  public ApiResponse<List<Post>> getPostsByUserId(int userId) {
    APIResponse response = request.get(
        "/posts", RequestOptions.create().setQueryParam("userId", userId));
    return toListResponse(response);
  }

  public ApiResponse<Post> createPost(Post post) {
    APIResponse response = request.post(
        "/posts", RequestOptions.create().setData(post));
    return toResponse(response, Post.class);
  }

  private <T> ApiResponse<T> toResponse(APIResponse response, Class<T> type) {
    String raw = response.text();
    log.debug("{} {} -> {} {}", response.url(), type.getSimpleName(), response.status(), raw);
    T body = null;
    if (response.status() < 400 && !raw.isBlank()) {
      try {
        body = MAPPER.readValue(raw, type);
      } catch (Exception e) {
        log.warn("Could not parse response body as {}: {}", type.getSimpleName(), e.getMessage());
      }
    }
    return new ApiResponse<>(response.status(), body, raw);
  }

  private ApiResponse<List<Post>> toListResponse(APIResponse response) {
    String raw = response.text();
    log.debug("{} -> {} {}", response.url(), response.status(), raw);
    List<Post> body = null;
    if (response.status() < 400 && !raw.isBlank()) {
      try {
        body = MAPPER.readValue(raw, MAPPER.getTypeFactory()
            .constructCollectionType(List.class, Post.class));
      } catch (Exception e) {
        log.warn("Could not parse response body as List<Post>: {}", e.getMessage());
      }
    }
    return new ApiResponse<>(response.status(), body, raw);
  }
}
