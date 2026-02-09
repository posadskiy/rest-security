package example;

import com.posadskiy.restsecurity.context.SecurityContextHolder;
import com.posadskiy.restsecurity.enforcer.SecurityEnforcer;
import com.posadskiy.restsecurity.exception.RestSecurityException;
import com.posadskiy.restsecurity.jwt.JwtConfig;
import com.posadskiy.restsecurity.jwt.JwtSecurityController;
import com.posadskiy.restsecurity.rest.SecuredRequest;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.List;
import java.util.Map;

/**
 * Lightweight HTTP server with JWT auth. No session store or user DB; user and roles come from JWT claims.
 */
public class Main {

    private static final String HMAC_SECRET = "my-hmac-secret-at-least-256-bits-long-for-hs256-alignment";

    public static void main(String[] args) {
        JwtConfig config = JwtConfig.withSecret(HMAC_SECRET);
        JwtSecurityController jwt = new JwtSecurityController(config);
        SecurityEnforcer enforcer = new SecurityEnforcer(jwt, jwt);
        JwtService jwtService = new JwtService(HMAC_SECRET);

        Javalin app = Javalin.create(javalinConfig -> javalinConfig.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()))).start(8080);

        app.get("/health", ctx -> ctx.json(Map.of("status", "UP")));

        app.post("/login", ctx -> {
            String body = ctx.body();
            String username = body.contains("alice") ? "alice" : body.contains("bob") ? "bob" : body.contains("admin") ? "admin" : null;
            if (username == null) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Invalid credentials"));
                return;
            }
            List<String> roles = username.equals("admin") ? List.of("ADMIN") : username.equals("bob") ? List.of("USER", "EDITOR") : List.of("USER");
            String token = jwtService.createToken(username, roles, 3600);
            ctx.json(Map.of("token", token));
        });

        app.get("/users", ctx -> {
            String auth = ctx.header("Authorization");
            String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
            SecuredRequestContext request = new SecuredRequest(token);
            try {
                String list = enforcer.enforceAndCall(request, new String[]{"USER"}, () -> {
                    String userId = SecurityContextHolder.getContext().userId();
                    return "Users list (for " + userId + ")";
                });
                ctx.json(Map.of("data", list));
            } catch (RestSecurityException e) {
                ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", e.getMessage()));
            }
        });

        app.get("/admin", ctx -> {
            String auth = ctx.header("Authorization");
            String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
            SecuredRequestContext request = new SecuredRequest(token);
            try {
                enforcer.enforceAndCall(request, new String[]{"ADMIN"}, () -> "ok");
                ctx.json(Map.of("message", "Admin area"));
            } catch (RestSecurityException e) {
                ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", e.getMessage()));
            }
        });

        System.out.println("Example 02 running at http://localhost:8080");
        System.out.println("  POST /login → returns JWT in body");
        System.out.println("  GET /users  -H 'Authorization: Bearer <token>'");
        System.out.println("  GET /admin  (requires ADMIN)");
    }
}
