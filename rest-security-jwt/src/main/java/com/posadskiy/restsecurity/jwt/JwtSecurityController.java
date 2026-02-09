package com.posadskiy.restsecurity.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.posadskiy.restsecurity.controller.SessionSecurityController;
import com.posadskiy.restsecurity.controller.UserSecurityController;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Base64;

/**
 * Implements both {@link SessionSecurityController} and {@link UserSecurityController}
 * using JWT claims. The "session" is the JWT token itself; user and roles come from claims.
 * Use with {@link com.posadskiy.restsecurity.enforcer.SecurityEnforcer} for zero-infrastructure auth.
 */
public final class JwtSecurityController implements SessionSecurityController, UserSecurityController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtConfig config;
    private final SecretKey secretKey;
    /** Single-slot cache: last token string and its parsed result, to avoid ThreadLocal lifecycle issues. */
    private volatile String lastTokenKey;
    private volatile ParsedToken lastParsedToken;

    public JwtSecurityController(JwtConfig config) {
        this.config = config;
        if (config.trustGateway()) {
            this.secretKey = null;
        } else if (config.secret() != null && !config.secret().isBlank()) {
            this.secretKey = Keys.hmacShaKeyFor(config.secret().getBytes(StandardCharsets.UTF_8));
        } else {
            this.secretKey = null;
        }
    }

    @Override
    public boolean isSessionExist(String token) {
        return parseToken(token) != null;
    }

    @Override
    public boolean isSessionExpired(String token) {
        ParsedToken parsed = parseToken(token);
        return parsed == null || parsed.expired;
    }

    @Override
    public String getUserIdBySessionId(String token) {
        ParsedToken parsed = parseToken(token);
        if (parsed == null) {
            return null;
        }
        lastTokenKey = token;
        lastParsedToken = parsed;
        return parsed.userId;
    }

    @Override
    public boolean isUserExist(String userId) {
        return userId != null && !userId.isBlank();
    }

    @Override
    public List<String> getUserRoles(String userId) {
        ParsedToken parsed = lastParsedToken;
        if (parsed != null && userId.equals(parsed.userId)) {
            lastParsedToken = null;
            lastTokenKey = null;
            return parsed.roles;
        }
        return Collections.emptyList();
    }

    private ParsedToken parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            if (config.trustGateway()) {
                return parseUnverified(token);
            }
            if (secretKey == null) {
                return null;
            }
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return toParsedToken(claims, false);
        } catch (ExpiredJwtException e) {
            return toParsedToken(e.getClaims(), true);
        } catch (JwtException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private ParsedToken parseUnverified(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            if (payloadBytes == null || payloadBytes.length == 0) {
                return null;
            }
            Map<String, Object> map = OBJECT_MAPPER.readValue(payloadBytes, Map.class);
            String userId = map.get(config.userIdClaim()) != null
                    ? map.get(config.userIdClaim()).toString()
                    : null;
            Object rolesObj = map.get(config.rolesClaim());
            List<String> roles = toRolesList(rolesObj);
            Long exp = map.get("exp") instanceof Number n ? n.longValue() : null;
            boolean expired = exp != null && exp * 1000L < System.currentTimeMillis();
            return new ParsedToken(userId, roles, expired);
        } catch (Exception e) {
            return null;
        }
    }

    private ParsedToken toParsedToken(Claims claims, boolean expired) {
        String userId = claims.get(config.userIdClaim(), String.class);
        if (userId == null) {
            Object sub = claims.get(config.userIdClaim());
            userId = sub != null ? sub.toString() : null;
        }
        Object rolesObj = claims.get(config.rolesClaim());
        List<String> roles = toRolesList(rolesObj);
        return new ParsedToken(userId, roles, expired);
    }

    @SuppressWarnings("unchecked")
    private static List<String> toRolesList(Object rolesObj) {
        if (rolesObj == null) {
            return Collections.emptyList();
        }
        if (rolesObj instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    out.add(o.toString());
                }
            }
            return out;
        }
        if (rolesObj instanceof String s) {
            return List.of(s);
        }
        return Collections.emptyList();
    }

    private record ParsedToken(String userId, List<String> roles, boolean expired) {}
}
