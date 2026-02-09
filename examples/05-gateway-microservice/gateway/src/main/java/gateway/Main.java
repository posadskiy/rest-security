package gateway;

import io.javalin.Javalin;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Mock API gateway: login returns a signed JWT; downstream service trusts this gateway (trustGateway=true).
 */
public class Main {

    public static void main(String[] args) {
        String secret = System.getenv().getOrDefault("JWT_SECRET",
                "gateway-secret-at-least-256-bits-for-hs256-signing");
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Javalin app = Javalin.create(javalinConfig -> javalinConfig.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()))).start(8080);

        app.get("/health", ctx -> ctx.json(Map.of("status", "UP", "role", "gateway")));

        app.post("/login", ctx -> {
            String body = ctx.body();
            String username = body.contains("alice") ? "alice" : body.contains("bob") ? "bob" : body.contains("admin") ? "admin" : null;
            if (username == null) {
                ctx.status(401).json(Map.of("error", "Invalid credentials"));
                return;
            }
            List<String> roles = username.equals("admin") ? List.of("ADMIN") : username.equals("bob") ? List.of("USER", "EDITOR") : List.of("USER");
            long now = System.currentTimeMillis();
            String token = Jwts.builder()
                    .subject(username)
                    .claim("roles", roles)
                    .issuedAt(new Date(now))
                    .expiration(new Date(now + 3600_000))
                    .signWith(key)
                    .compact();
            ctx.json(Map.of("token", token));
        });

        System.out.println("Gateway running at http://localhost:8080 (login returns JWT)");
    }
}
