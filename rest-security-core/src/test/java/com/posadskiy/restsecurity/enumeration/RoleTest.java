package com.posadskiy.restsecurity.enumeration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void isAdmin_returnsTrueForAdmin() {
        assertTrue(Role.isAdmin("ADMIN"));
    }

    @Test
    void isAdmin_returnsFalseForOther() {
        assertFalse(Role.isAdmin("USER"));
        assertFalse(Role.isAdmin(""));
        assertFalse(Role.isAdmin(null));
    }

    @Test
    void getRole_returnsConstantValue() {
        assertEquals("USER", Role.USER.getRole());
        assertEquals("ADMIN", Role.ADMIN.getRole());
    }
}
