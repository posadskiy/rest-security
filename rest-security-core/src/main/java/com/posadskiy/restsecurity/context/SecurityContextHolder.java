package com.posadskiy.restsecurity.context;

/**
 * Thread-local holder for the current security context.
 * Automatically populated by the security interceptor for @Security-annotated methods.
 *
 * <p>Usage within a secured method:
 * <pre>
 * SecurityContext ctx = SecurityContextHolder.getContext();
 * String userId = ctx.userId();
 * boolean isAdmin = ctx.hasRole("ADMIN");
 * </pre>
 */
public final class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> CONTEXT = new ThreadLocal<>();

    private SecurityContextHolder() {
    }

    /**
     * Get the security context for the current thread.
     * @return current context, or null if not in a secured method
     */
    public static SecurityContext getContext() {
        return CONTEXT.get();
    }

    /**
     * Set the security context for the current thread.
     * Internal use only; called by the security interceptor.
     */
    public static void setContext(SecurityContext context) {
        if (context == null) {
            CONTEXT.remove();
        } else {
            CONTEXT.set(context);
        }
    }

    /**
     * Clear the security context for the current thread.
     * Internal use only; called by the security interceptor.
     */
    public static void clearContext() {
        CONTEXT.remove();
    }
}
