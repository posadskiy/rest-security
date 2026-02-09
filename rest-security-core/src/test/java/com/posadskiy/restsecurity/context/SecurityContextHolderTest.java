package com.posadskiy.restsecurity.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextHolderTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getContext_initiallyNull() {
        assertNull(SecurityContextHolder.getContext());
    }

    @Test
    void setContext_andGetContext_returnsSetValue() {
        var ctx = new SecurityContext("s1", "u1", java.util.Set.of("USER"));
        SecurityContextHolder.setContext(ctx);
        assertSame(ctx, SecurityContextHolder.getContext());
    }

    @Test
    void setContext_null_removesContext() {
        SecurityContextHolder.setContext(new SecurityContext("s1", "u1", java.util.Set.of()));
        SecurityContextHolder.setContext(null);
        assertNull(SecurityContextHolder.getContext());
    }

    @Test
    void clearContext_removesContext() {
        SecurityContextHolder.setContext(new SecurityContext("s1", "u1", java.util.Set.of()));
        SecurityContextHolder.clearContext();
        assertNull(SecurityContextHolder.getContext());
    }
}
