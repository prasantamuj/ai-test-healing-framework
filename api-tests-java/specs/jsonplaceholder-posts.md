# API Test Plan: Posts

**Target:** https://jsonplaceholder.typicode.com/posts
**Google:** api-tests-java/src/test/java/com/aitest/api/tests/GoogleApiTest.java
**Date:** 2026-09-05

## Overview

Covers the `/posts` resource on the free JSONPlaceholder demo API: fetching a single post,
handling a not-found id, filtering by `userId`, and creating a post. All scenarios are
independent and require no auth (JSONPlaceholder is unauthenticated).

## Preconditions

- API is reachable at https://jsonplaceholder.typicode.com
- No auth required (`auth.token` is blank in `application.properties`)
- Known-good google data: post id `1` belongs to `userId` `1` and always exists

## Scenarios

### Scenario 1.1 — GET a single post by id
- **Priority:** P0
- **Tags:** @smoke @critical
- **Method & path:** `GET /posts/1`
- **Preconditions:** none
- **Request:** no body
- **Expected response:** `200 OK`, JSON object with `id`, `userId`, `title`, `body`
- **Assertions:**
  - Status is `200`
  - `id` equals `1`
  - `userId` is a positive integer
  - `title` is non-blank
- **Edge cases considered:** none — golden path

### Scenario 1.2 — GET a post with an id that does not exist
- **Priority:** P1
- **Tags:** @regression
- **Method & path:** `GET /posts/999999`
- **Preconditions:** id `999999` does not exist
- **Expected response:** `404 Not Found`
- **Assertions:**
  - Status is `404`
- **Edge cases considered:** distinguishes "not found" from a malformed-id `400` — this API
  returns `404` for any non-existent numeric id, not `400`

### Scenario 1.3 — GET posts filtered by userId
- **Priority:** P1
- **Tags:** @regression
- **Method & path:** `GET /posts?userId=1`
- **Preconditions:** none
- **Expected response:** `200 OK`, JSON array where every item's `userId` is `1`
- **Assertions:**
  - Status is `200`
  - Array is non-empty
  - Every item's `userId` equals `1` (not just the first — a loose check on one item would miss a
    filter that silently stopped working)

### Scenario 1.4 — POST creates a new post
- **Priority:** P1
- **Tags:** @regression
- **Method & path:** `POST /posts`
- **Preconditions:** none
- **Request:** `{ "userId": 1, "title": "...", "body": "..." }`
- **Expected response:** `201 Created`, echoes `title`/`body`, assigns a new `id`
- **Assertions:**
  - Status is `201`
  - Response `title`/`body` match what was sent
  - Response `id` is a positive integer (JSONPlaceholder fakes persistence — it always returns
    `101` for a new post, but assert `> 0` rather than the literal `101` so the test doesn't break
    if the fake backend's counter behavior changes)

## Not covered (and why)

- `PUT`/`PATCH`/`DELETE` on `/posts/{id}` — JSONPlaceholder fakes these too (no real persistence),
  low value to cover here; would belong in a dedicated mutation-focused plan if this were a real
  backend with a verifiable side effect
- Auth/authorization scenarios — this API has none; a real, authenticated API should add a
  dedicated `Scenario 2.x` block for 401/403 handling
- Rate limiting — JSONPlaceholder doesn't enforce any; not testable against this target
