package com.posadskiy.restsecurity.context;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextTest {

    @Test
    void hasRole_returnsTrueWhenRolePresent() {
        var ctx = new SecurityContext("s1", "u1", Set.of("USER", "EDITOR"));
        assertTrue(ctx.hasRole("USER"));
        assertTrue(ctx.hasRole("EDITOR"));
        assertFalse(ctx.hasRole("ADMIN"));
    }

    @Test
    void hasAnyRole_returnsTrueWhenOneMatches() {
        var ctx = new SecurityContext("s1", "u1", Set.of("USER"));
        assertTrue(ctx.hasAnyRole("USER"));
        assertTrue(ctx.hasAnyRole("ADMIN", "USER"));
        assertFalse(ctx.hasAnyRole("ADMIN", "EDITOR"));
    }

    @Test
    void hasAllRoles_returnsTrueOnlyWhenAllMatch() {
        var ctx = new SecurityContext("s1", "u1", Set.of("USER", "EDITOR"));
        assertTrue(ctx.hasAllRoles("USER"));
        assertTrue(ctx.hasAllRoles("USER", "EDITOR"));
        assertFalse(ctx.hasAllRoles("USER", "ADMIN"));
    }

    @Test
    void nullRoles_treatedAsEmpty() {
        var ctx = new SecurityContext("s1", "u1", null);
        assertEquals(Set.of(), ctx.roles());
        assertFalse(ctx.hasRole("USER"));
    }

    @Test
    void recordAccessors() {
        var ctx = new SecurityContext("session-1", "user-1", Set.of("ADMIN"));
        assertEquals("session-1", ctx.sessionId());
        assertEquals("user-1", ctx.userId());
        assertEquals(Set.of("ADMIN"), ctx.roles());
    }
}
