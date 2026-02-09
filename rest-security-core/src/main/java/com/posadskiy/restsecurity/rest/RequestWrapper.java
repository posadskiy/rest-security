package com.posadskiy.restsecurity.rest;

/**
 * Mutable request wrapper for @Security-annotated methods.
 *
 * @deprecated Prefer immutable {@link SecuredRequest} for new code.
 */
@Deprecated(since = "0.4.0", forRemoval = false)
public class RequestWrapper implements SecuredRequestContext {

    private Object data;
    private String userId;
    private String sessionId;

    public RequestWrapper data(Object data) {
        this.data = data;
        return this;
    }

    public RequestWrapper userId(String userId) {
        this.userId = userId;
        return this;
    }

    public RequestWrapper sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public Object getData() {
        return data;
    }
}
