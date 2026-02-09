package com.posadskiy.restsecurity.controller;

import java.util.List;
import java.util.Set;

/**
 * User and role management contract for security validation.
 * Implement this interface to integrate your user store.
 */
public interface UserSecurityController {

    /**
     * Check if a user exists.
     * @param userId user identifier
     * @return true if user exists
     */
    boolean isUserExist(String userId);

    /**
     * Get all roles assigned to a user.
     * @param userId user identifier
     * @return list of role names (never null; empty if user has no roles)
     */
    List<String> getUserRoles(String userId);

    /**
     * Get all roles as a Set for efficient lookup.
     * @param userId user identifier
     * @return set of role names (never null; empty if user has no roles)
     */
    default Set<String> getUserRolesSet(String userId) {
        List<String> roles = getUserRoles(userId);
        return roles != null ? Set.copyOf(roles) : Set.of();
    }
}
