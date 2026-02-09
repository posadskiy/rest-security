package com.posadskiy.restsecurity.rest;

/**
 * Immutable request context for @Security-annotated methods.
 * Use this instead of mutating RequestWrapper for better security and clarity.
 *
 * @param sessionId required; session identifier (e.g. from cookie or Authorization header)
 * @param userId    optional; when set, non-admin callers may only access their own user
 * @param data     optional request payload
 */
public record SecuredRequest(String sessionId, String userId, Object data) implements SecuredRequestContext {

    @Override
    public String getSessionId() { return sessionId; }

    @Override
    public String getUserId() { return userId; }

    @Override
    public Object getData() { return data; }

    public SecuredRequest(String sessionId) {
        this(sessionId, null, null);
    }

    public SecuredRequest(String sessionId, Object data) {
        this(sessionId, null, data);
    }
}
