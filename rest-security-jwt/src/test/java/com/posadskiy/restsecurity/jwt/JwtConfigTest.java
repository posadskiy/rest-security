package com.posadskiy.restsecurity.jwt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtConfigTest {

    @Test
    void withSecret_setsDefaults() {
        JwtConfig config = JwtConfig.withSecret("my-secret");
        assertEquals("my-secret", config.secret());
        assertEquals("roles", config.rolesClaim());
        assertEquals("sub", config.userIdClaim());
        assertFalse(config.trustGateway());
    }

    @Test
    void withSecretTrustGateway_allowsBlankSecret() {
        JwtConfig config = JwtConfig.withSecretTrustGateway("unused");
        assertTrue(config.trustGateway());
        assertEquals("roles", config.rolesClaim());
        assertEquals("sub", config.userIdClaim());
    }

    @Test
    void constructor_nullSecretWhenNotTrustGateway_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new JwtConfig(null, "roles", "sub", false));
    }

    @Test
    void constructor_blankSecretWhenNotTrustGateway_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new JwtConfig("", "roles", "sub", false));
        assertThrows(IllegalArgumentException.class, () ->
                new JwtConfig("   ", "roles", "sub", false));
    }

    @Test
    void constructor_nullRolesClaim_defaultsToRoles() {
        JwtConfig config = new JwtConfig("secret", null, "sub", false);
        assertEquals("roles", config.rolesClaim());
    }

    @Test
    void constructor_blankUserIdClaim_defaultsToSub() {
        JwtConfig config = new JwtConfig("secret", "roles", "  ", false);
        assertEquals("sub", config.userIdClaim());
    }
}
