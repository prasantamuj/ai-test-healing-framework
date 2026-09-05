package com.aitest.api.model;

/**
 * Typed wrapper around a Playwright {@code APIResponse} — status code plus the parsed body.
 * Service Objects return this (or a plain parsed model); they never assert anything themselves.
 */
public final class ApiResponse<T> {

  private final int status;
  private final T body;
  private final String rawBody;

  public ApiResponse(int status, T body, String rawBody) {
    this.status = status;
    this.body = body;
    this.rawBody = rawBody;
  }

  public int status() {
    return status;
  }

  public T body() {
    return body;
  }

  public String rawBody() {
    return rawBody;
  }
}
