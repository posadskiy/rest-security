package service;

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
 * Microservice that trusts the gateway: JwtConfig.withSecretTrustGateway(...).
 * Token is read from X-JWT-Token (gateway forwards it); no signature verification.
 */
public class Main {

    private static final String JWT_HEADER = System.getenv().getOrDefault("GATEWAY_JWT_HEADER", "X-JWT-Token");

    public static void main(String[] args) {
        JwtConfig config = JwtConfig.withSecretTrustGateway("unused-secret-not-verified");
        JwtSecurityController jwt = new JwtSecurityController(config);
        SecurityEnforcer enforcer = new SecurityEnforcer(jwt, jwt);

        Javalin app = Javalin.create(javalinConfig -> javalinConfig.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()))).start(8081);

        app.get("/health", ctx -> ctx.json(Map.of("status", "UP", "role", "service")));

        app.get("/orders", ctx -> {
            String token = ctx.header(JWT_HEADER);
            SecuredRequestContext request = new SecuredRequest(token);
            try {
                List<String> orders = enforcer.enforceAndCall(request, new String[]{"USER"}, () -> {
                    String userId = SecurityContextHolder.getContext().userId();
                    return List.of("order-1-" + userId, "order-2-" + userId);
                });
                ctx.json(Map.of("orders", orders));
            } catch (RestSecurityException e) {
                ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", e.getMessage()));
            }
        });

        app.get("/admin/stats", ctx -> {
            String token = ctx.header(JWT_HEADER);
            SecuredRequestContext request = new SecuredRequest(token);
            try {
                enforcer.enforceAndCall(request, new String[]{"ADMIN"}, () -> null);
                ctx.json(Map.of("stats", "admin-only"));
            } catch (RestSecurityException e) {
                ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", e.getMessage()));
            }
        });

        System.out.println("Service running at http://localhost:8081 (expects " + JWT_HEADER + " from gateway)");
    }
}
