package com.posadskiy.restsecurity.enumeration;

/**
 * Built-in roles. ADMIN bypasses role checks and same-user restriction.
 */
public enum Role {
    USER("USER"),
    ADMIN("ADMIN");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getRole() {
        return value;
    }

    /** Role name equal to the constant. */
    public static boolean isAdmin(String role) {
        return ADMIN.value.equals(role);
    }
}
