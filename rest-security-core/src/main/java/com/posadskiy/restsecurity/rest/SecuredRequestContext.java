package com.posadskiy.restsecurity.rest;

/**
 * Immutable context for a secured request: session identity and optional target user.
 * Prefer {@link SecuredRequest} (record) for new code.
 */
public interface SecuredRequestContext {

    String getSessionId();

    String getUserId();

    Object getData();
}
