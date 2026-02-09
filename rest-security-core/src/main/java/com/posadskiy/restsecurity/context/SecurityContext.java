package com.posadskiy.restsecurity.context;

import java.util.Set;

/**
 * Immutable security context for the current request.
 * Available within @Security-annotated methods via {@link SecurityContextHolder}.
 *
 * @param sessionId session identifier
 * @param userId authenticated user identifier
 * @param roles user's roles
 */
public record SecurityContext(String sessionId, String userId, Set<String> roles) {

    public SecurityContext {
        roles = roles != null ? Set.copyOf(roles) : Set.of();
    }

    /**
     * Check if the user has a specific role.
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Check if the user has any of the given roles.
     */
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the user has all of the given roles.
     */
    public boolean hasAllRoles(String... roles) {
        for (String role : roles) {
            if (!this.roles.contains(role)) {
                return false;
            }
        }
        return true;
    }
}
