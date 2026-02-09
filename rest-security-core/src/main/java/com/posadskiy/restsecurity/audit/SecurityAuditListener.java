package com.posadskiy.restsecurity.audit;

import com.posadskiy.restsecurity.context.SecurityContext;
import com.posadskiy.restsecurity.exception.RestSecurityException;

/**
 * Optional listener for security events (authentication success/failure).
 * Register as a Spring bean (when using rest-security-spring) to receive callbacks.
 */
public interface SecurityAuditListener {

    /**
     * Called when security validation succeeds.
     * @param context authenticated security context
     * @param method method name being invoked
     */
    default void onAuthenticationSuccess(SecurityContext context, String method) {
    }

    /**
     * Called when security validation fails.
     * @param sessionId session identifier (may be null/blank)
     * @param method method name being invoked
     * @param exception security exception thrown
     */
    default void onAuthenticationFailure(String sessionId, String method, RestSecurityException exception) {
    }
}
