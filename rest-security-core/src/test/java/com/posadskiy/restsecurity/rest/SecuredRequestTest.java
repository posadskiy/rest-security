package com.posadskiy.restsecurity.rest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecuredRequestTest {

    @Test
    void constructor_sessionIdOnly_setsUserIdAndDataNull() {
        var req = new SecuredRequest("session-1");
        assertEquals("session-1", req.getSessionId());
        assertNull(req.getUserId());
        assertNull(req.getData());
    }

    @Test
    void constructor_sessionIdAndData() {
        var req = new SecuredRequest("session-1", "payload");
        assertEquals("session-1", req.getSessionId());
        assertNull(req.getUserId());
        assertEquals("payload", req.getData());
    }

    @Test
    void fullConstructor() {
        var req = new SecuredRequest("session-1", "user-1", "data");
        assertEquals("session-1", req.getSessionId());
        assertEquals("user-1", req.getUserId());
        assertEquals("data", req.getData());
    }
}
