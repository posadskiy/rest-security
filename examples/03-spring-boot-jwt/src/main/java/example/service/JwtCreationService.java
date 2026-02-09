package example.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtCreationService {

    private final SecretKey secretKey;
    private static final String ROLES_CLAIM = "roles";
    private static final long VALIDITY_SECONDS = 3600;

    public JwtCreationService(@Value("${app.jwt.secret:spring-boot-jwt-example-secret-at-least-256-bits-for-hs256}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createTokenForUser(String username) {
        List<String> roles = switch (username) {
            case "admin" -> List.of("ADMIN");
            case "bob" -> List.of("USER", "EDITOR");
            case "alice" -> List.of("USER");
            default -> null;
        };
        if (roles == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim(ROLES_CLAIM, roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + VALIDITY_SECONDS * 1000))
                .signWith(secretKey)
                .compact();
    }
}
