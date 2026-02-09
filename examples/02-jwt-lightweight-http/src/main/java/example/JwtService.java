package example;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Creates HMAC-signed JWTs for login. Uses same secret as JwtSecurityController for verification.
 */
public class JwtService {

    private final SecretKey secretKey;
    private static final String ROLES_CLAIM = "roles";
    private static final String USER_ID_CLAIM = "sub";

    public JwtService(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String userId, List<String> roles, long validitySeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(secretKey)
                .compact();
    }
}
