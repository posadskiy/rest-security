package com.posadskiy.restsecurity.controller;

import java.util.Optional;

/**
 * Session management contract for security validation.
 * Implement this interface to integrate your session store.
 */
public interface SessionSecurityController {

    /**
     * Check if a session exists in the store.
     * @param sessionId session identifier
     * @return true if session exists
     */
    boolean isSessionExist(String sessionId);

    /**
     * Check if a session has expired.
     * @param sessionId session identifier
     * @return true if session is expired
     */
    boolean isSessionExpired(String sessionId);

    /**
     * Get the user ID associated with a session.
     * @param sessionId session identifier
     * @return user ID, or empty if session has no user
     */
    default Optional<String> getUserId(String sessionId) {
        return Optional.ofNullable(getUserIdBySessionId(sessionId));
    }

    /**
     * @deprecated Use {@link #getUserId(String)} for Optional support
     */
    @Deprecated(since = "0.4.0", forRemoval = false)
    String getUserIdBySessionId(String sessionId);
}
