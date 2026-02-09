package com.posadskiy.restsecurity.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtSecurityControllerTest {

    private static final String SECRET = "test-secret-key-at-least-256-bits-long-for-hs256";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Test
    void validTokenWithRoles_validationSucceeds() {
        String token = Jwts.builder()
                .subject("user123")
                .claim("roles", List.of("USER", "EDITOR"))
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(KEY)
                .compact();

        JwtConfig config = JwtConfig.withSecret(SECRET);
        JwtSecurityController controller = new JwtSecurityController(config);

        assertTrue(controller.isSessionExist(token));
        assertFalse(controller.isSessionExpired(token));
        assertEquals("user123", controller.getUserIdBySessionId(token));
        assertTrue(controller.isUserExist("user123"));
        List<String> roles = controller.getUserRoles("user123");
        assertTrue(roles.contains("USER"));
        assertTrue(roles.contains("EDITOR"));
    }

    @Test
    void expiredToken_isSessionExpiredReturnsTrue() {
        String token = Jwts.builder()
                .subject("user123")
                .claim("roles", List.of("USER"))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(KEY)
                .compact();

        JwtSecurityController controller = new JwtSecurityController(JwtConfig.withSecret(SECRET));

        assertTrue(controller.isSessionExpired(token));
    }

    @Test
    void invalidToken_isSessionExistReturnsFalse() {
        JwtSecurityController controller = new JwtSecurityController(JwtConfig.withSecret(SECRET));

        assertFalse(controller.isSessionExist("not.a.jwt"));
        assertFalse(controller.isSessionExist(null));
        assertFalse(controller.isSessionExist(""));
    }

    @Test
    void trustGateway_mode_acceptsTokenWithoutVerification() {
        String token = Jwts.builder()
                .subject("user456")
                .claim("roles", List.of("ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(KEY)
                .compact();

        JwtConfig config = JwtConfig.withSecretTrustGateway("any-secret-not-used-for-verification");
        JwtSecurityController controller = new JwtSecurityController(config);

        assertTrue(controller.isSessionExist(token));
        assertEquals("user456", controller.getUserIdBySessionId(token));
        List<String> roles = controller.getUserRoles("user456");
        assertTrue(roles.contains("ADMIN"));
    }

    @Test
    void customClaimNames_rolesClaimAndUserIdClaim_usedCorrectly() {
        String token = Jwts.builder()
                .claim("custom_sub", "uid-99")
                .claim("custom_roles", List.of("EDITOR"))
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(KEY)
                .compact();

        JwtConfig config = new JwtConfig(SECRET, "custom_roles", "custom_sub", false);
        JwtSecurityController controller = new JwtSecurityController(config);

        assertTrue(controller.isSessionExist(token));
        assertEquals("uid-99", controller.getUserIdBySessionId(token));
        List<String> roles = controller.getUserRoles("uid-99");
        assertTrue(roles.contains("EDITOR"));
    }

    @Test
    void tokenWithNoRolesClaim_returnsEmptyRoles() {
        String token = Jwts.builder()
                .subject("user-no-roles")
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(KEY)
                .compact();

        JwtSecurityController controller = new JwtSecurityController(JwtConfig.withSecret(SECRET));

        assertTrue(controller.isSessionExist(token));
        assertEquals("user-no-roles", controller.getUserIdBySessionId(token));
        List<String> roles = controller.getUserRoles("user-no-roles");
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    void tokenWithRolesAsSingleString_parsedAsSingleElementList() {
        String token = Jwts.builder()
                .subject("user-single-role")
                .claim("roles", "MANAGER")
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(KEY)
                .compact();

        JwtSecurityController controller = new JwtSecurityController(JwtConfig.withSecret(SECRET));

        assertEquals("user-single-role", controller.getUserIdBySessionId(token));
        List<String> roles = controller.getUserRoles("user-single-role");
        assertEquals(List.of("MANAGER"), roles);
    }

    @Test
    void isUserExist_nullReturnsFalse() {
        JwtSecurityController controller = new JwtSecurityController(JwtConfig.withSecret(SECRET));
        assertFalse(controller.isUserExist(null));
    }

    @Test
    void isUserExist_blankReturnsFalse() {
        JwtSecurityController controller = new JwtSecurityController(JwtConfig.withSecret(SECRET));
        assertFalse(controller.isUserExist(""));
        assertFalse(controller.isUserExist("   "));
    }

    @Test
    void invalidSignature_isSessionExistReturnsFalse() {
        String token = Jwts.builder()
                .subject("user123")
                .claim("roles", List.of("USER"))
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(KEY)
                .compact();
        JwtSecurityController controller = new JwtSecurityController(
                JwtConfig.withSecret("different-secret-at-least-256-bits-long-for-hs256"));
        assertFalse(controller.isSessionExist(token));
    }

    @Test
    void getUserRoles_withoutPriorGetUserIdBySessionId_returnsEmpty() {
        JwtSecurityController controller = new JwtSecurityController(JwtConfig.withSecret(SECRET));
        assertTrue(controller.getUserRoles("any").isEmpty());
    }

    @Test
    void getUserRoles_differentUserIdThanCached_returnsEmpty() {
        String token = Jwts.builder()
                .subject("user-a")
                .claim("roles", List.of("USER"))
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(KEY)
                .compact();
        JwtSecurityController controller = new JwtSecurityController(JwtConfig.withSecret(SECRET));
        controller.getUserIdBySessionId(token);
        List<String> roles = controller.getUserRoles("user-b");
        assertTrue(roles.isEmpty());
    }

    @Test
    void trustGateway_tokenWithSinglePart_isSessionExistReturnsFalse() {
        JwtConfig config = JwtConfig.withSecretTrustGateway("x");
        JwtSecurityController controller = new JwtSecurityController(config);
        assertFalse(controller.isSessionExist("onlyonepart"));
    }

    @Test
    void trustGateway_tokenWithInvalidBase64Payload_isSessionExistReturnsFalse() {
        JwtConfig config = JwtConfig.withSecretTrustGateway("x");
        JwtSecurityController controller = new JwtSecurityController(config);
        assertFalse(controller.isSessionExist("header.!!!not-valid-base64!!!.sig"));
    }

    @Test
    void isSessionExpired_nullToken_returnsTrue() {
        JwtSecurityController controller = new JwtSecurityController(JwtConfig.withSecret(SECRET));
        assertTrue(controller.isSessionExpired(null));
    }

    @Test
    void getUserIdBySessionId_invalidToken_returnsNull() {
        JwtSecurityController controller = new JwtSecurityController(JwtConfig.withSecret(SECRET));
        assertNull(controller.getUserIdBySessionId("bad.token.here"));
    }
}
